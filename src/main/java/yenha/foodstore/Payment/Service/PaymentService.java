package yenha.foodstore.Payment.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yenha.foodstore.Order.Entity.Order;
import yenha.foodstore.Order.Entity.OrderStatus;
import yenha.foodstore.Order.Repository.OrderRepository;
import yenha.foodstore.Payment.DTO.PaymentEventDTO;
import yenha.foodstore.Payment.DTO.SepayWebhookDTO;
import yenha.foodstore.Payment.Entity.Payment;
import yenha.foodstore.Payment.Entity.PaymentStatus;
import yenha.foodstore.Payment.Repository.PaymentRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final SSEService sseService;

    private static final Pattern ORDER_ID_PATTERN = Pattern.compile("YHF\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    
    /**
     * Xử lý webhook từ SePay
     * @param webhook Data từ SePay
     */
    @Transactional
    public void processWebhook(SepayWebhookDTO webhook) {

        if (paymentRepository.existsBySepayTransactionId(webhook.getId())) {
            return;
        }
        if (!validateWebhook(webhook)) {
            log.error("Invalid webhook data - transferType: {}, amount: {}",
                webhook.getTransferType(), webhook.getTransferAmount());
            saveFailedPayment(webhook, "Invalid webhook data");
            return;
        }

        log.info("Extracting orderId from code='{}' or content='{}'...",
            webhook.getCode(), webhook.getContent());
        Long orderId = extractOrderId(webhook);

        if (orderId == null) {
            log.error("Cannot extract orderId from webhook. Code='{}', Content='{}'",
                webhook.getCode(), webhook.getContent());
            saveFailedPayment(webhook, "Cannot extract orderId");
            return;
        }

        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            log.error("Order not found in database: orderId={}", orderId);
            saveFailedPayment(webhook, "Order not found: " + orderId);
            sseService.sendPaymentEvent(orderId, PaymentEventDTO.failed(orderId, "Không tìm thấy đơn hàng"));
            return;
        }

        Order order = orderOpt.get();
        log.info("Order details: id={}, status={}, totalAmount={}",
            order.getOrderId(), order.getStatus(), order.getTotalAmount());

        if (order.getStatus() != OrderStatus.SERVED) {
            log.error("Order status is not SERVED. Current status: {}, orderId: {}",
                order.getStatus(), orderId);
            saveFailedPayment(webhook, "Order must be SERVED before payment. Current status: " + order.getStatus());
            sseService.sendPaymentEvent(orderId, PaymentEventDTO.failed(orderId, "Đơn hàng chưa được phục vụ"));
            return;
        }

        log.info("Validating amount: expected={}, received={}",
            order.getTotalAmount(), webhook.getTransferAmount());
        if (!validateAmount(webhook.getTransferAmount(), order.getTotalAmount())) {
            log.error("Amount mismatch. Expected: {}, Got: {}, Difference: {}",
                order.getTotalAmount(), webhook.getTransferAmount(),
                Math.abs(webhook.getTransferAmount() - order.getTotalAmount()));
            saveAmountMismatchPayment(webhook, orderId);
            sseService.sendPaymentEvent(orderId, PaymentEventDTO.failed(orderId, "Số tiền không khớp"));
            return;
        }
        Payment payment = saveSuccessfulPayment(webhook, orderId);

        updateOrderStatus(order);

        PaymentEventDTO event = PaymentEventDTO.success(
            orderId,
            payment.getId(),
            payment.getTransferAmount(),
            payment.getGateway(),
            payment.getTransactionDate().toString()
        );

        sseService.sendPaymentEvent(orderId, event);

    }
    
    /**
     * Validate webhook data
     */
    private boolean validateWebhook(SepayWebhookDTO webhook) {
        if (!"in".equalsIgnoreCase(webhook.getTransferType())) {
            log.warn("Invalid transferType: {}", webhook.getTransferType());
            return false;
        }

        if (webhook.getTransferAmount() == null || webhook.getTransferAmount() <= 0) {
            log.warn("Invalid transferAmount: {}", webhook.getTransferAmount());
            return false;
        }
        
        return true;
    }
    
    /**
     * Extract orderId từ content hoặc code
     */
    private Long extractOrderId(SepayWebhookDTO webhook) {
        if (webhook.getCode() != null && !webhook.getCode().isEmpty()) {
            try {
                return Long.parseLong(webhook.getCode());
            } catch (NumberFormatException e) {

                Matcher matcher = ORDER_ID_PATTERN.matcher(webhook.getCode());
                if (matcher.find()) {
                    try {
                        return Long.parseLong(matcher.group(1));
                    } catch (NumberFormatException e2) {
                        log.warn("Cannot parse orderId from code regex match: {}", matcher.group(1));
                    }
                }
            }
        }

        if (webhook.getContent() != null) {
            Matcher matcher = ORDER_ID_PATTERN.matcher(webhook.getContent());
            if (matcher.find()) {
                try {
                    return Long.parseLong(matcher.group(1));
                } catch (NumberFormatException e) {
                    log.warn("Cannot parse orderId from content regex match: {}", matcher.group(1));
                }
            }
        }

        log.error("Failed to extract orderId from code='{}' or content='{}'",
            webhook.getCode(), webhook.getContent());
        return null;
    }
    
    /**
     * Validate số tiền
     */
    private boolean validateAmount(Double transferAmount, Double expectedAmount) {
        if (transferAmount == null || expectedAmount == null) {
            return false;
        }
        
        double difference = Math.abs(transferAmount - expectedAmount);
        return difference <= 0;
    }
    
    /**
     * Lưu payment thành công
     */
    private Payment saveSuccessfulPayment(SepayWebhookDTO webhook, Long orderId) {
        Payment payment = new Payment();
        payment.setSepayTransactionId(webhook.getId());
        payment.setOrderId(orderId);
        payment.setGateway(webhook.getGateway());
        payment.setTransactionDate(parseTransactionDate(webhook.getTransactionDate()));
        payment.setAccountNumber(webhook.getAccountNumber());
        payment.setContent(webhook.getContent());
        payment.setTransferAmount(webhook.getTransferAmount());
        payment.setReferenceCode(webhook.getReferenceCode());
        payment.setDescription(webhook.getDescription());
        payment.setStatus(PaymentStatus.SUCCESS);
        
        return paymentRepository.save(payment);
    }
    
    /**
     * Lưu payment khi số tiền không khớp
     */
    private void saveAmountMismatchPayment(SepayWebhookDTO webhook, Long orderId) {
        Payment payment = new Payment();
        payment.setSepayTransactionId(webhook.getId());
        payment.setOrderId(orderId);
        payment.setGateway(webhook.getGateway());
        payment.setTransactionDate(parseTransactionDate(webhook.getTransactionDate()));
        payment.setAccountNumber(webhook.getAccountNumber());
        payment.setContent(webhook.getContent());
        payment.setTransferAmount(webhook.getTransferAmount());
        payment.setReferenceCode(webhook.getReferenceCode());
        payment.setDescription(webhook.getDescription());
        payment.setStatus(PaymentStatus.AMOUNT_MISMATCH);
        payment.setErrorMessage("Amount mismatch");
        
        paymentRepository.save(payment);
    }
    
    /**
     * Lưu payment khi thất bại
     */
    private void saveFailedPayment(SepayWebhookDTO webhook, String errorMessage) {
        Payment payment = new Payment();
        payment.setSepayTransactionId(webhook.getId());
        payment.setGateway(webhook.getGateway());
        payment.setTransactionDate(parseTransactionDate(webhook.getTransactionDate()));
        payment.setAccountNumber(webhook.getAccountNumber());
        payment.setContent(webhook.getContent());
        payment.setTransferAmount(webhook.getTransferAmount());
        payment.setReferenceCode(webhook.getReferenceCode());
        payment.setDescription(webhook.getDescription());
        payment.setStatus(PaymentStatus.FAILED);
        payment.setErrorMessage(errorMessage);
        
        paymentRepository.save(payment);
    }
    
    /**
     * Parse transaction date từ String sang LocalDateTime
     */
    private LocalDateTime parseTransactionDate(String dateStr) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return LocalDateTime.parse(dateStr, formatter);
        } catch (Exception e) {
            log.error("Error parsing transaction date: {}", dateStr, e);
            return LocalDateTime.now();
        }
    }
    
    /**
     * Cập nhật trạng thái Order: SERVED → PAID
     */
    private void updateOrderStatus(Order order) {
        if (order.getStatus() == OrderStatus.SERVED) {
            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);
            log.info("Order status updated from SERVED to PAID for orderId: {}", order.getOrderId());
        } else {
            log.warn("Cannot update order status. Order is not SERVED. Current status: {}, orderId: {}", 
                order.getStatus(), order.getOrderId());
        }
    }
}



