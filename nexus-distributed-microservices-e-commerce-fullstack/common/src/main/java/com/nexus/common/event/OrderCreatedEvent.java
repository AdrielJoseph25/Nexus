package com.nexus.common.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        String customerId,
        String sku,
        int quantity,
        BigDecimal amount,
        Instant occurredAt
) {
}
