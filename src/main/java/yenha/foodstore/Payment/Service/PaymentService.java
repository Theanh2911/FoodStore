package yenha.foodstore.Payment.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
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
    
    // Pattern để extract orderId từ content
    // VD: "YHF123", "DH123", "Order 123", "don hang 123"
    private static final Pattern ORDER_ID_PATTERN = Pattern.compile("(?:YHF|DH|Order|don hang|donhang)\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    
    /**
     * Xử lý webhook từ SePay (Async)
     * @param webhook Data từ SePay
     */
    @Async
    @Transactional
    public void processWebhook(SepayWebhookDTO webhook) {
        log.info("Processing webhook from SePay: {}", webhook.getId());
        
        try {
            // 1. Kiểm tra duplicate
            if (paymentRepository.existsBySepayTransactionId(webhook.getId())) {
                log.warn("Duplicate webhook detected, sepayTransactionId: {}", webhook.getId());
                return;
            }
            
            // 2. Validate webhook
            if (!validateWebhook(webhook)) {
                log.error("Invalid webhook data: {}", webhook);
                saveFailedPayment(webhook, "Invalid webhook data");
                return;
            }
            
            // 3. Extract orderId từ content hoặc code
            Long orderId = extractOrderId(webhook);
            log.info("Extracted orderId: {} from webhook. Code: {}, Content: {}", orderId, webhook.getCode(), webhook.getContent());
            
            if (orderId == null) {
                log.error("Cannot extract orderId from webhook: {}", webhook.getContent());
                saveFailedPayment(webhook, "Cannot extract orderId");
                return;
            }
            
            // 4. Kiểm tra Order tồn tại
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                log.error("Order not found: {}", orderId);
                saveFailedPayment(webhook, "Order not found: " + orderId);
                sseService.sendPaymentEvent(orderId, PaymentEventDTO.failed(orderId, "Không tìm thấy đơn hàng"));
                return;
            }
            
            Order order = orderOpt.get();
            log.info("Found order: id={}, status={}, amount={}", order.getOrderId(), order.getStatus(), order.getTotalAmount());
            
            // 5. Kiểm tra order phải ở trạng thái SERVED mới cho thanh toán
            if (order.getStatus() != OrderStatus.SERVED) {
                log.error("Order status is not SERVED. Current status: {}, orderId: {}", order.getStatus(), orderId);
                saveFailedPayment(webhook, "Order must be SERVED before payment. Current status: " + order.getStatus());
                sseService.sendPaymentEvent(orderId, PaymentEventDTO.failed(orderId, "Đơn hàng chưa được phục vụ"));
                return;
            }
            
            // 6. Validate số tiền
            if (!validateAmount(webhook.getTransferAmount(), order.getTotalAmount())) {
                log.error("Amount mismatch. Expected: {}, Got: {}", order.getTotalAmount(), webhook.getTransferAmount());
                saveAmountMismatchPayment(webhook, orderId, "Amount mismatch");
                sseService.sendPaymentEvent(orderId, PaymentEventDTO.failed(orderId, "Số tiền không khớp"));
                return;
            }
            
            // 7. Lưu Payment thành công
            Payment payment = saveSuccessfulPayment(webhook, orderId);
            
            // 8. Cập nhật Order status: SERVED → PAID
            updateOrderStatus(order);
            
            // 9. Gửi SSE event cho FE
            PaymentEventDTO event = PaymentEventDTO.success(
                orderId, 
                payment.getId(), 
                payment.getTransferAmount(),
                payment.getGateway(),
                payment.getTransactionDate().toString()
            );
            
            log.info("Sending SSE event for orderId: {}, event: {}", orderId, event);
            sseService.sendPaymentEvent(orderId, event);
            
            log.info("Payment processed successfully for orderId: {}", orderId);
            
        } catch (Exception e) {
            log.error("Error processing webhook: ", e);
        }
    }
    
    /**
     * Validate webhook data
     */
    private boolean validateWebhook(SepayWebhookDTO webhook) {
        // Kiểm tra chỉ nhận tiền vào
        if (!"in".equalsIgnoreCase(webhook.getTransferType())) {
            log.warn("Invalid transferType: {}", webhook.getTransferType());
            return false;
        }
        
        // Kiểm tra số tiền > 0
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
        // Ưu tiên lấy từ code nếu có
        if (webhook.getCode() != null && !webhook.getCode().isEmpty()) {
            try {
                return Long.parseLong(webhook.getCode());
            } catch (NumberFormatException e) {
                log.warn("Cannot parse orderId from code: {}", webhook.getCode());
            }
        }
        
        // Nếu không có code, parse từ content
        if (webhook.getContent() != null) {
            Matcher matcher = ORDER_ID_PATTERN.matcher(webhook.getContent());
            if (matcher.find()) {
                try {
                    return Long.parseLong(matcher.group(1));
                } catch (NumberFormatException e) {
                    log.warn("Cannot parse orderId from content: {}", webhook.getContent());
                }
            }
        }
        
        return null;
    }
    
    /**
     * Validate số tiền (cho phép sai số 1000 VND)
     */
    private boolean validateAmount(Double transferAmount, Double expectedAmount) {
        if (transferAmount == null || expectedAmount == null) {
            return false;
        }
        
        double difference = Math.abs(transferAmount - expectedAmount);
        return difference <= 1000; // Cho phép sai số 1000 VND
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
    private void saveAmountMismatchPayment(SepayWebhookDTO webhook, Long orderId, String errorMessage) {
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
        payment.setErrorMessage(errorMessage);
        
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

