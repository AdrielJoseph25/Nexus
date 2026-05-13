package com.nexus.order.service;

import com.nexus.common.dto.CreateOrderRequest;
import com.nexus.common.event.InventoryRejectedEvent;
import com.nexus.common.event.InventoryReleaseEvent;
import com.nexus.common.event.InventoryReservedEvent;
import com.nexus.common.event.OrderCreatedEvent;
import com.nexus.common.event.PaymentCompletedEvent;
import com.nexus.common.event.PaymentFailedEvent;
import com.nexus.common.event.Topics;
import com.nexus.order.domain.CustomerOrder;
import com.nexus.order.repository.OrderRepository;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderSagaService {
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderSagaService(OrderRepository orderRepository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.orderRepository = orderRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public CustomerOrder createOrder(CreateOrderRequest request) {
        if (request.quantity() <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        CustomerOrder order = orderRepository.save(new CustomerOrder(
                UUID.randomUUID(),
                request.customerId(),
                request.sku(),
                request.quantity(),
                request.amount()
        ));
        kafkaTemplate.send(Topics.ORDER_CREATED, order.getId().toString(), new OrderCreatedEvent(
                order.getId(),
                order.getCustomerId(),
                order.getSku(),
                order.getQuantity(),
                order.getAmount(),
                Instant.now()
        ));
        return order;
    }

    public List<CustomerOrder> listOrders() {
        return orderRepository.findAll();
    }

    @KafkaListener(topics = Topics.INVENTORY_RESERVED)
    @Transactional
    public void onInventoryReserved(InventoryReservedEvent event) {
        orderRepository.findById(event.orderId()).ifPresent(order -> {
            order.markInventoryReserved();
            orderRepository.save(order);
        });
    }

    @KafkaListener(topics = Topics.INVENTORY_REJECTED)
    @Transactional
    public void onInventoryRejected(InventoryRejectedEvent event) {
        orderRepository.findById(event.orderId()).ifPresent(order -> {
            order.cancel(event.reason());
            orderRepository.save(order);
        });
    }

    @KafkaListener(topics = Topics.PAYMENT_COMPLETED)
    @Transactional
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        orderRepository.findById(event.orderId()).ifPresent(order -> {
            order.confirm();
            orderRepository.save(order);
        });
    }

    @KafkaListener(topics = Topics.PAYMENT_FAILED)
    @Transactional
    public void onPaymentFailed(PaymentFailedEvent event) {
        orderRepository.findById(event.orderId()).ifPresent(order -> {
            order.cancel(event.reason());
            orderRepository.save(order);
            kafkaTemplate.send(Topics.INVENTORY_RELEASE, event.orderId().toString(), new InventoryReleaseEvent(
                    event.orderId(),
                    event.sku(),
                    event.quantity(),
                    event.reason(),
                    Instant.now()
            ));
        });
    }
}
