package yenha.foodstore.Order.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import yenha.foodstore.Constant.Error;
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
        OrderResponseDTO testOrder = new OrderResponseDTO();
        testOrder.setOrderId(999L);
        testOrder.setCustomerName("Thử khách hàng");
        testOrder.setTableNumber(1);
        testOrder.setTotalAmount(25.99);
        testOrder.setOrderTime(java.time.LocalDateTime.now());
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder.setItems(new java.util.ArrayList<>());
        
        orderEventService.broadcastOrderCreated(testOrder);
        return ResponseEntity.ok("reload database event test");
    }

    @PostMapping("/create")
    public ResponseEntity<?> createOrder(@RequestBody OrderDTO orderDTO, @RequestParam(required = false) String sessionId) {
        try {
            if (orderDTO.getName() == null || orderDTO.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Error.ORDER_CUSTOMER_NAME_BLANK);
            }
            
            if (sessionId != null && !sessionId.trim().isEmpty()) {
                orderDTO.setSessionId(sessionId);
            }

            if ((orderDTO.getSessionId() == null || orderDTO.getSessionId().trim().isEmpty()) && 
                (orderDTO.getTableNumber() == null || orderDTO.getTableNumber() <= 0)) {
                return ResponseEntity.badRequest().body("Table number is required when no session is provided");
            }
            
            if (orderDTO.getTotal() == null || orderDTO.getTotal() <= 0) {
                return ResponseEntity.badRequest().body(Error.ORDER_TOTAL_INVALID);
            }
            
            if (orderDTO.getItems() == null || orderDTO.getItems().isEmpty()) {
                return ResponseEntity.badRequest().body(Error.ORDER_ITEMS_EMPTY);
            }

            for (var item : orderDTO.getItems()) {
                if (item.getQuantity() == null || item.getQuantity() <= 0) {
                    return ResponseEntity.badRequest().body(Error.ORDER_ITEM_QUANTITY_INVALID);
                }
            }
            
            Order createdOrder = orderService.createOrder(orderDTO);
            OrderResponseDTO responseDTO = orderService.convertToResponseDTO(createdOrder);

            orderEventService.broadcastOrderCreated(responseDTO);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(e.getMessage());
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

            orderEventService.broadcastOrderStatusChanged(responseDTO);
            
            return ResponseEntity.ok(responseDTO);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

}
