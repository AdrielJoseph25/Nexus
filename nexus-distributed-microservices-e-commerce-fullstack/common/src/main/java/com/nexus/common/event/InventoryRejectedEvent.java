package com.nexus.common.event;

import java.time.Instant;
import java.util.UUID;

public record InventoryRejectedEvent(
        UUID orderId,
        String sku,
        int requestedQuantity,
        String reason,
        Instant occurredAt
) {
}
