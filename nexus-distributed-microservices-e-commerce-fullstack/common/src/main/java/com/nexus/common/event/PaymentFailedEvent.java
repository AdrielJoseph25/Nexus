package com.nexus.common.event;

import java.time.Instant;
import java.util.UUID;

public record PaymentFailedEvent(
        UUID orderId,
        String sku,
        int quantity,
        String reason,
        Instant occurredAt
) {
}
