package com.nexus.payment.service;

import com.nexus.common.event.InventoryReservedEvent;
import com.nexus.common.event.PaymentCompletedEvent;
import com.nexus.common.event.PaymentFailedEvent;
import com.nexus.common.event.Topics;
import com.nexus.payment.domain.Payment;
import com.nexus.payment.domain.PaymentStatus;
import com.nexus.payment.repository.PaymentRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {
    private static final BigDecimal SINGLE_PAYMENT_LIMIT = new BigDecimal("5000.00");

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentService(PaymentRepository paymentRepository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.paymentRepository = paymentRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    public List<Payment> list() {
        return paymentRepository.findAll();
    }

    @KafkaListener(topics = Topics.INVENTORY_RESERVED)
    @Transactional
    public void onInventoryReserved(InventoryReservedEvent event) {
        if (event.amount().compareTo(SINGLE_PAYMENT_LIMIT) > 0) {
            Payment failed = paymentRepository.save(new Payment(
                    UUID.randomUUID(),
                    event.orderId(),
                    event.amount(),
                    PaymentStatus.FAILED,
                    "payment amount exceeds authorization limit"
            ));
            kafkaTemplate.send(Topics.PAYMENT_FAILED, event.orderId().toString(), new PaymentFailedEvent(
                    failed.getOrderId(),
                    event.sku(),
                    event.quantity(),
                    failed.getFailureReason(),
                    Instant.now()
            ));
            return;
        }

        Payment completed = paymentRepository.save(new Payment(
                UUID.randomUUID(),
                event.orderId(),
                event.amount(),
                PaymentStatus.COMPLETED,
                null
        ));
        kafkaTemplate.send(Topics.PAYMENT_COMPLETED, event.orderId().toString(), new PaymentCompletedEvent(
                completed.getOrderId(),
                completed.getId(),
                Instant.now()
        ));
    }
}
