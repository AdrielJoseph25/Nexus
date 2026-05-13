package com.nexus.inventory.service;

import com.nexus.common.event.InventoryRejectedEvent;
import com.nexus.common.event.InventoryReleaseEvent;
import com.nexus.common.event.InventoryReservedEvent;
import com.nexus.common.event.OrderCreatedEvent;
import com.nexus.common.event.Topics;
import com.nexus.inventory.domain.InventoryItem;
import com.nexus.inventory.repository.InventoryRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public InventoryService(InventoryRepository inventoryRepository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.inventoryRepository = inventoryRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostConstruct
    void seedStock() {
        if (inventoryRepository.count() == 0) {
            inventoryRepository.save(new InventoryItem("NX-LAPTOP-17", 25));
            inventoryRepository.save(new InventoryItem("NX-HEADSET-PRO", 100));
            inventoryRepository.save(new InventoryItem("NX-DOCK-4K", 40));
        }
    }

    public List<InventoryItem> list() {
        return inventoryRepository.findAll();
    }

    @KafkaListener(topics = Topics.ORDER_CREATED)
    @Transactional
    public void onOrderCreated(OrderCreatedEvent event) {
        InventoryItem item = inventoryRepository.findById(event.sku())
                .orElseGet(() -> new InventoryItem(event.sku(), 0));
        if (item.reserve(event.quantity())) {
            inventoryRepository.save(item);
            kafkaTemplate.send(Topics.INVENTORY_RESERVED, event.orderId().toString(), new InventoryReservedEvent(
                    event.orderId(),
                    event.customerId(),
                    event.sku(),
                    event.quantity(),
                    event.amount(),
                    Instant.now()
            ));
            return;
        }
        kafkaTemplate.send(Topics.INVENTORY_REJECTED, event.orderId().toString(), new InventoryRejectedEvent(
                event.orderId(),
                event.sku(),
                event.quantity(),
                "insufficient stock",
                Instant.now()
        ));
    }

    @KafkaListener(topics = Topics.INVENTORY_RELEASE)
    @Transactional
    public void onInventoryRelease(InventoryReleaseEvent event) {
        inventoryRepository.findById(event.sku()).ifPresent(item -> {
            item.release(event.quantity());
            inventoryRepository.save(item);
        });
    }
}
