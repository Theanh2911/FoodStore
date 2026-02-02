package yenha.foodstore.Order.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yenha.foodstore.Auth.Entity.OrderSession;
import yenha.foodstore.Auth.Entity.User;
import yenha.foodstore.Auth.Service.OrderSessionService;
import yenha.foodstore.Auth.Service.UserService;
import yenha.foodstore.Inventory.Entity.DailyProductInventory;
import yenha.foodstore.Inventory.Service.InventoryService;
import yenha.foodstore.Inventory.Service.InventorySSEService;
import yenha.foodstore.Menu.Entity.Product;
import yenha.foodstore.Menu.Repository.ProductRepository;
import yenha.foodstore.Order.DTO.OrderDTO;
import yenha.foodstore.Order.DTO.OrderItemDTO;
import yenha.foodstore.Order.DTO.OrderResponseDTO;
import yenha.foodstore.Order.DTO.OrderItemResponseDTO;
import yenha.foodstore.Order.Entity.Order;
import yenha.foodstore.Order.Entity.OrderItem;
import yenha.foodstore.Order.Entity.OrderStatus;
import yenha.foodstore.Order.Repository.OrderRepository;
import yenha.foodstore.Order.Repository.OrderItemRepository;
import yenha.foodstore.Promotion.Service.PromotionService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final OrderSessionService orderSessionService;
    private final UserService userService;
    private final InventoryService inventoryService;
    private final InventorySSEService inventorySSEService;
    private final PromotionService promotionService;

    @Transactional
    public Order createOrder(OrderDTO orderDTO) {

        Order order = new Order();
        LocalDate today = LocalDate.now();

        // Auto-map customer name from userId if provided
        String customerName = orderDTO.getName(); // Default to provided name
        if (orderDTO.getUserId() != null && !orderDTO.getUserId().trim().isEmpty()) {
            try {
                Long userId = Long.parseLong(orderDTO.getUserId());
                Optional<User> userOpt = userService.findById(userId);
                if (userOpt.isPresent()) {
                    customerName = userOpt.get().getName(); // Use user's name from database
                    order.setUserId(orderDTO.getUserId());
                } else {
                    throw new RuntimeException("User not found with ID: " + orderDTO.getUserId());
                }
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid user ID format: " + orderDTO.getUserId());
            }
        }

        order.setCustomerName(customerName);

        Integer tableNumber = orderDTO.getTableNumber();
        if (orderDTO.getSessionId() != null && !orderDTO.getSessionId().trim().isEmpty()) {
            OrderSession session = orderSessionService.getSession(orderDTO.getSessionId());
            if (session != null && session.getIsActive()) {
                tableNumber = session.getTableNumber();
            } else {
                throw new RuntimeException("Invalid or inactive session: " + orderDTO.getSessionId());
            }
        }

        order.setTableNumber(tableNumber);
        order.setOrderTime(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);

        // Calculate total amount from products
        double totalAmount = 0.0;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemDTO itemDTO : orderDTO.getItems()) {
            Optional<Product> productOpt = productRepository.findById(itemDTO.getProductId());
            if (!productOpt.isPresent()) {
                throw new RuntimeException("Product not found with ID: " + itemDTO.getProductId());
            }

            Product product = productOpt.get();

            // Validate product is active (not soft deleted)
            if (product.getIsActive() == null || !product.getIsActive()) {
                throw new RuntimeException("Product is no longer available: " + product.getName());
            }

            // CHECK AND DECREASE INVENTORY (with optimistic lock)
            DailyProductInventory inventory = inventoryService.decreaseInventory(
                    product.getProductId(),
                    itemDTO.getQuantity(),
                    today);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemDTO.getQuantity());
            orderItem.setPriceAtPurchase(product.getPrice()); // Save price at purchase time
            orderItem.setProductNameAtPurchase(product.getName()); // Save product name at purchase time
            orderItem.setNote(itemDTO.getNote());

            // Calculate total: price * quantity
            totalAmount += product.getPrice() * itemDTO.getQuantity();
            orderItems.add(orderItem);

            // BROADCAST INVENTORY UPDATE via SSE
            inventorySSEService.broadcastInventoryUpdate(
                    product.getProductId(),
                    inventory.getNumberRemain());
        }

        // Set calculated total amount
        order.setTotalAmount(totalAmount);

        // Apply promotion if provided
        double discountAmount = 0.0;
        if (orderDTO.getPromotionCode() != null && !orderDTO.getPromotionCode().trim().isEmpty()) {
            // Temporarily set items to calculate discount
            order.setItems(orderItems);

            // Apply promotion and get discount amount
            discountAmount = promotionService.applyPromotion(
                    orderDTO.getPromotionCode(),
                    totalAmount,
                    orderItems);

            order.setPromotionCode(orderDTO.getPromotionCode().toUpperCase());
        }

        order.setDiscountAmount(discountAmount);
        order.setFinalAmount(totalAmount - discountAmount);

        // Save order first to get the ID
        order = orderRepository.save(order);

        // Now save order items
        orderItemRepository.saveAll(orderItems);
        order.setItems(orderItems);

        return order;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Optional<Order> getOrderById(Long orderId) {
        return orderRepository.findById(orderId);
    }

    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    public List<Order> getOrdersByTableNumber(Integer tableNumber) {
        return orderRepository.findByTableNumber(tableNumber);
    }

    public List<Order> getOrdersByUserId(String userId) {
        return orderRepository.findByUserIdOrderByOrderTimeDesc(userId);
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus status) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            order.setStatus(status);
            return orderRepository.save(order);
        }
        throw new RuntimeException("Order not found with id: " + orderId);
    }

    public OrderResponseDTO convertToResponseDTO(Order order) {
        OrderResponseDTO responseDTO = new OrderResponseDTO();
        responseDTO.setOrderId(order.getOrderId());
        responseDTO.setCustomerName(order.getCustomerName());
        responseDTO.setTableNumber(order.getTableNumber());

        // Set amount (finalAmount) - primary amount field to use
        double finalAmount = order.getFinalAmount() != null ? order.getFinalAmount() : order.getTotalAmount();
        responseDTO.setAmount(finalAmount);

        // Keep these for detailed breakdown
        responseDTO.setTotalAmount(order.getTotalAmount());
        responseDTO.setPromotionCode(order.getPromotionCode());
        responseDTO.setDiscountAmount(order.getDiscountAmount() != null ? order.getDiscountAmount() : 0.0);
        responseDTO.setFinalAmount(finalAmount); // For backward compatibility

        responseDTO.setOrderTime(order.getOrderTime());
        responseDTO.setStatus(order.getStatus());
        responseDTO.setIsRated(order.getIsRated() != null ? order.getIsRated() : false);

        if (order.getItems() != null) {
            List<OrderItemResponseDTO> itemDTOs = order.getItems().stream()
                    .map(this::convertToItemResponseDTO)
                    .collect(Collectors.toList());
            responseDTO.setItems(itemDTOs);
        }

        return responseDTO;
    }

    private OrderItemResponseDTO convertToItemResponseDTO(OrderItem orderItem) {
        OrderItemResponseDTO itemDTO = new OrderItemResponseDTO();
        itemDTO.setOrderItemId(orderItem.getOrderItemId());
        itemDTO.setProductId(orderItem.getProduct().getProductId());
        itemDTO.setProductName(orderItem.getProductNameAtPurchase()); // Use saved name at purchase time
        itemDTO.setProductPrice(orderItem.getPriceAtPurchase()); // Use price at purchase, not current price
        itemDTO.setQuantity(orderItem.getQuantity());
        itemDTO.setNote(orderItem.getNote());
        return itemDTO;
    }
}
