package yenha.foodstore.Order.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import yenha.foodstore.Order.DTO.OrderDTO;
import yenha.foodstore.Order.DTO.OrderResponseDTO;
import yenha.foodstore.Order.DTO.StatusUpdateDTO;
import yenha.foodstore.Order.Entity.Order;
import yenha.foodstore.Order.Entity.OrderStatus;
import yenha.foodstore.Order.Service.OrderEventService;
import yenha.foodstore.Order.Service.OrderService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderService orderService;
    private final OrderEventService orderEventService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamOrders(@RequestParam(defaultValue = "default") String clientId) {
        return orderEventService.createEmitter(clientId);
    }

    @PostMapping("/test-sse")
    public ResponseEntity<String> testSSE() {
        // Create a dummy order for testing SSE
        OrderResponseDTO testOrder = new OrderResponseDTO();
        testOrder.setOrderId(999L);
        testOrder.setCustomerName("Test Customer");
        testOrder.setTableNumber(1);
        testOrder.setTotalAmount(25.99);
        testOrder.setOrderTime(java.time.LocalDateTime.now());
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder.setItems(new java.util.ArrayList<>());
        
        orderEventService.broadcastOrderCreated(testOrder);
        return ResponseEntity.ok("Test SSE event sent");
    }

    @PostMapping("/create")
    public ResponseEntity<?> createOrder(@RequestBody OrderDTO orderDTO) {
        try {
            if (orderDTO.getName() == null || orderDTO.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Customer name is required");
            }
            
            if (orderDTO.getTotal() == null || orderDTO.getTotal() <= 0) {
                return ResponseEntity.badRequest().body("Total amount must be greater than 0");
            }
            
            if (orderDTO.getItems() == null || orderDTO.getItems().isEmpty()) {
                return ResponseEntity.badRequest().body("Order must contain at least one item");
            }

            for (var item : orderDTO.getItems()) {
                if (item.getProductId() == null) {
                    return ResponseEntity.badRequest().body("Product ID is required for each item");
                }
                if (item.getQuantity() == null || item.getQuantity() <= 0) {
                    return ResponseEntity.badRequest().body("Quantity must be greater than 0 for each item");
                }
            }
            
            Order createdOrder = orderService.createOrder(orderDTO);
            OrderResponseDTO responseDTO = orderService.convertToResponseDTO(createdOrder);
            
            // Broadcast order creation event via SSE
            orderEventService.broadcastOrderCreated(responseDTO);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error creating order: " + e.getMessage());
        }
    }

    @GetMapping("/getAll")
    public ResponseEntity<List<OrderResponseDTO>> getAllOrders() {
        List<Order> orders = orderService.getAllOrders();
        List<OrderResponseDTO> responseDTOs = orders.stream()
            .map(orderService::convertToResponseDTO)
            .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderById(@PathVariable Long orderId) {
        Optional<Order> order = orderService.getOrderById(orderId);
        if (order.isPresent()) {
            OrderResponseDTO responseDTO = orderService.convertToResponseDTO(order.get());
            return ResponseEntity.ok(responseDTO);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderResponseDTO>> getOrdersByStatus(@PathVariable OrderStatus status) {
        List<Order> orders = orderService.getOrdersByStatus(status);
        List<OrderResponseDTO> responseDTOs = orders.stream()
            .map(orderService::convertToResponseDTO)
            .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/table/{tableNumber}")
    public ResponseEntity<List<OrderResponseDTO>> getOrdersByTableNumber(@PathVariable Integer tableNumber) {
        List<Order> orders = orderService.getOrdersByTableNumber(tableNumber);
        List<OrderResponseDTO> responseDTOs = orders.stream()
            .map(orderService::convertToResponseDTO)
            .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long orderId, @RequestBody StatusUpdateDTO statusUpdate) {
        try {
            Order updatedOrder = orderService.updateOrderStatus(orderId, statusUpdate.getStatus());
            OrderResponseDTO responseDTO = orderService.convertToResponseDTO(updatedOrder);
            
            // Broadcast order status change event via SSE
            orderEventService.broadcastOrderStatusChanged(responseDTO);
            
            return ResponseEntity.ok(responseDTO);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

}
