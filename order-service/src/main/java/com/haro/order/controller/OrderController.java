package com.haro.order.controller;

import com.haro.order.dto.CreateOrderRequest;
import com.haro.order.dto.OrderResponse;
import com.haro.order.dto.OrderStatus;
import com.haro.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse orderResponse = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderResponse);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.getOrder(id));
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getOrdersForUser(
            @RequestParam UUID userId,
            @RequestParam(required = false) OrderStatus status,
            Pageable pageable
    ) {
        return ResponseEntity.ok(orderService.getOrdersForUser(userId, status, pageable));
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<OrderResponse> markOrderPaid(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.markOrderPaid(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.cancelOrder(id));
    }

    @PostMapping("/{id}/fail")
    public ResponseEntity<OrderResponse> failOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.failOrder(id, "Manual fail via API"));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<OrderResponse> refundOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.refundOrder(id));
    }
}
