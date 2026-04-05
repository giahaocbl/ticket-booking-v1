package com.haro.order.service;

import com.haro.common.web.ConflictException;
import com.haro.common.web.NotFoundException;
import com.haro.order.dto.*;
import com.haro.order.entity.Order;
import com.haro.order.entity.OrderItem;
import com.haro.order.event.OrderCreatedEvent;
import com.haro.order.event.OrderFailedEvent;
import com.haro.order.event.OrderPaidEvent;
import com.haro.order.event.SagaTopics;
import com.haro.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OutboxService outboxService;

    public OrderService(OrderRepository orderRepository, OutboxService outboxService) {
        this.orderRepository = orderRepository;
        this.outboxService = outboxService;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Order existing = orderRepository.findByIdempotencyKey(request.idempotencyKey()).orElse(null);
        if (existing != null) {
            return toResponse(existing);
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        Order order = Order.builder()
                .userId(request.userId())
                .reservationId(request.reservationId())
                .status(Order.Status.PENDING)
                .currency(request.currency().toUpperCase())
                .idempotencyKey(request.idempotencyKey())
                .build();

        for (CreateOrderItemRequest itemReq : request.items()) {
            BigDecimal itemTotal = itemReq.unitPrice().multiply(BigDecimal.valueOf(itemReq.quantity()));

            OrderItem item = OrderItem.builder()
                    .eventId(itemReq.eventId())
                    .eventOccurrenceId(itemReq.eventOccurrenceId())
                    .ticketTypeId(itemReq.ticketTypeId())
                    .eventTitle(itemReq.eventTitle())
                    .occurrenceStartsAt(itemReq.occurrenceStartsAt())
                    .ticketTypeName(itemReq.ticketTypeName())
                    .quantity(itemReq.quantity())
                    .unitPrice(itemReq.unitPrice())
                    .totalPrice(itemTotal)
                    .reservationId(itemReq.reservationId())
                    .build();

            order.addItem(item);
            totalAmount = totalAmount.add(itemTotal);
        }

        order.setTotalAmount(totalAmount);

        try {
            order = orderRepository.save(order);
        } catch (DataIntegrityViolationException ex) {
            Order concurrent = orderRepository.findByIdempotencyKey(request.idempotencyKey())
                    .orElseThrow(() -> new ConflictException("Order conflicts with existing data"));
            return toResponse(concurrent);
        }

        outboxService.saveEvent(
                SagaTopics.ORDER,
                "ORDER",
                order.getId(),
                "OrderCreated",
                new OrderCreatedEvent(
                        order.getId(),
                        order.getUserId(),
                        order.getReservationId(),
                        order.getTotalAmount(),
                        order.getCurrency(),
                        order.getIdempotencyKey()
                )
        );

        log.info("Order created orderId={}, saga event queued", order.getId());
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID id) {
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersForUser(UUID userId, OrderStatus status, Pageable pageable) {
        Pageable effective = clampPageable(pageable);

        Page<Order> orders;
        if (status != null) {
            orders = orderRepository.findByUserIdAndStatus(userId, toEntityStatus(status), effective);
        } else {
            orders = orderRepository.findByUserId(userId, effective);
        }

        return orders.map(this::toResponse);
    }

    @Transactional
    public OrderResponse markOrderPaid(UUID id) {
        Order order = findOrderOrThrow(id);

        if (order.getStatus() == Order.Status.PAID) {
            return toResponse(order);
        }
        if (order.getStatus() != Order.Status.PENDING) {
            throw new ConflictException("Only pending orders can be marked as paid");
        }

        order.setStatus(Order.Status.PAID);
        order.setPaidAt(OffsetDateTime.now());
        order = orderRepository.save(order);

        outboxService.saveEvent(
                SagaTopics.ORDER_STATUS,
                "ORDER",
                order.getId(),
                "OrderPaid",
                new OrderPaidEvent(order.getId(), order.getUserId())
        );

        log.info("Order marked PAID orderId={}", order.getId());
        return toResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(UUID id) {
        Order order = findOrderOrThrow(id);

        if (order.getStatus() == Order.Status.CANCELED) {
            return toResponse(order);
        }
        if (order.getStatus() != Order.Status.PENDING) {
            throw new ConflictException("Only pending orders can be cancelled");
        }

        order.setStatus(Order.Status.CANCELED);
        order.setCancelledAt(OffsetDateTime.now());
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse failOrder(UUID id, String reason) {
        Order order = findOrderOrThrow(id);

        if (order.getStatus() == Order.Status.FAILED) {
            return toResponse(order);
        }
        if (order.getStatus() != Order.Status.PENDING) {
            throw new ConflictException("Only pending orders can be marked as failed");
        }

        order.setStatus(Order.Status.FAILED);
        order = orderRepository.save(order);

        outboxService.saveEvent(
                SagaTopics.ORDER_STATUS,
                "ORDER",
                order.getId(),
                "OrderFailed",
                new OrderFailedEvent(order.getId(), order.getUserId(), reason)
        );

        log.info("Order marked FAILED orderId={} reason={}", order.getId(), reason);
        return toResponse(order);
    }

    @Transactional
    public OrderResponse refundOrder(UUID id) {
        Order order = findOrderOrThrow(id);

        if (order.getStatus() == Order.Status.REFUNDED) {
            return toResponse(order);
        }
        if (order.getStatus() != Order.Status.PAID) {
            throw new ConflictException("Only paid orders can be refunded");
        }

        order.setStatus(Order.Status.REFUNDED);
        return toResponse(orderRepository.save(order));
    }

    private Order findOrderOrThrow(UUID id) {
        return orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new NotFoundException("Order not found"));
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getReservationId(),
                toDtoStatus(order.getStatus()),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getPaidAt(),
                order.getCancelledAt(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                items
        );
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getEventId(),
                item.getEventOccurrenceId(),
                item.getTicketTypeId(),
                item.getEventTitle(),
                item.getOccurrenceStartsAt(),
                item.getTicketTypeName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTotalPrice(),
                item.getReservationId()
        );
    }

    private OrderStatus toDtoStatus(Order.Status status) {
        return OrderStatus.valueOf(status.name());
    }

    private Order.Status toEntityStatus(OrderStatus status) {
        return Order.Status.valueOf(status.name());
    }

    private Pageable clampPageable(Pageable pageable) {
        int maxPageSize = 200;
        int size = Math.min(pageable.getPageSize(), maxPageSize);

        Sort sort = pageable.getSort();
        if (sort.isUnsorted()) {
            sort = Sort.by(Sort.Direction.DESC, "createdAt");
        }

        if (size == pageable.getPageSize() && sort.equals(pageable.getSort())) {
            return pageable;
        }

        return PageRequest.of(pageable.getPageNumber(), size, sort);
    }
}
