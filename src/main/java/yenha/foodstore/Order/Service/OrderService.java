package yenha.foodstore.Order.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yenha.foodstore.Auth.Entity.OrderSession;
import yenha.foodstore.Auth.Entity.User;
import yenha.foodstore.Auth.Service.OrderSessionService;
import yenha.foodstore.Auth.Service.UserService;
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

    @Transactional
    public Order createOrder(OrderDTO orderDTO) {

        Order order = new Order();
        
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
        order.setTotalAmount(orderDTO.getTotal());
        order.setOrderTime(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);

        order = orderRepository.save(order);

        List<OrderItem> orderItems = new ArrayList<>();
        for (OrderItemDTO itemDTO : orderDTO.getItems()) {
            Optional<Product> productOpt = productRepository.findById(itemDTO.getProductId());
            if (productOpt.isPresent()) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setProduct(productOpt.get());
                orderItem.setQuantity(itemDTO.getQuantity());
                orderItem.setNote(itemDTO.getNote());
                orderItems.add(orderItem);
            }
        }

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
        responseDTO.setTotalAmount(order.getTotalAmount());
        responseDTO.setOrderTime(order.getOrderTime());
        responseDTO.setStatus(order.getStatus());
        
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
        itemDTO.setProductName(orderItem.getProduct().getName());
        itemDTO.setProductPrice(orderItem.getProduct().getPrice());
        itemDTO.setQuantity(orderItem.getQuantity());
        itemDTO.setNote(orderItem.getNote());
        return itemDTO;
    }
}
