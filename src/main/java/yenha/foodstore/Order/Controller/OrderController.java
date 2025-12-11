package yenha.foodstore.Order.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    /**
     * Create order for authenticated users (require JWT)
     * Automatically extract userId from JWT token
     * Support both dine-in (with sessionId) and takeaway (with tableNumber or default)
     */
    @PostMapping("/create/authenticated")
    public ResponseEntity<?> createAuthenticatedOrder(@RequestBody OrderDTO orderDTO) {
        try {
            // Get authenticated user from SecurityContext
            org.springframework.security.core.Authentication authentication = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated() || 
                authentication.getPrincipal().equals("anonymousUser")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Authentication required. Please provide valid JWT token.");
            }
            
            // Extract phone number from authenticated user
            String phoneNumber = authentication.getName();
            
            // Basic validations
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
            
            // Create order for authenticated user
            // Logic: sessionId > tableNumber > default(0)
            Order createdOrder = orderService.createAuthenticatedOrder(orderDTO, phoneNumber);
            OrderResponseDTO responseDTO = orderService.convertToResponseDTO(createdOrder);

            orderEventService.broadcastOrderCreated(responseDTO);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(e.getMessage());
        }
    }

    /**
     * Create order for guest users (require sessionId)
     * Used for walk-in customers without login
     */
    @PostMapping("/create/guest")
    public ResponseEntity<?> createGuestOrder(@RequestBody OrderDTO orderDTO) {
        try {
            // Validate required fields for guest
            if (orderDTO.getName() == null || orderDTO.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Error.ORDER_CUSTOMER_NAME_BLANK);
            }
            
            if (orderDTO.getSessionId() == null || orderDTO.getSessionId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Session ID is required for guest orders");
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
            
            // Create order for guest
            Order createdOrder = orderService.createGuestOrder(orderDTO);
            OrderResponseDTO responseDTO = orderService.convertToResponseDTO(createdOrder);

            orderEventService.broadcastOrderCreated(responseDTO);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @GetMapping("/getAll")
    public ResponseEntity<List<OrderResponseDTO>> getAllOrders() {
        List<Order> orders = orderService.getAllOrders();
        List<OrderResponseDTO> responseDTOs = orders.stream()
            .map(orderService::convertToResponseDTO)
            .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderById(@PathVariable Long orderId) {
        Optional<Order> order = orderService.getOrderById(orderId);
        if (order.isPresent()) {
            OrderResponseDTO responseDTO = orderService.convertToResponseDTO(order.get());
            return ResponseEntity.ok(responseDTO);
        }
        return ResponseEntity.notFound().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderResponseDTO>> getOrdersByStatus(@PathVariable OrderStatus status) {
        List<Order> orders = orderService.getOrdersByStatus(status);
        List<OrderResponseDTO> responseDTOs = orders.stream()
            .map(orderService::convertToResponseDTO)
            .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @GetMapping("/table/{tableNumber}")
    public ResponseEntity<List<OrderResponseDTO>> getOrdersByTableNumber(@PathVariable Integer tableNumber) {
        List<Order> orders = orderService.getOrdersByTableNumber(tableNumber);
        List<OrderResponseDTO> responseDTOs = orders.stream()
            .map(orderService::convertToResponseDTO)
            .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
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
