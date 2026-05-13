package com.nexus.common.event;

import java.time.Instant;
import java.util.UUID;

public record InventoryReleaseEvent(
        UUID orderId,
        String sku,
        int quantity,
        String reason,
        Instant occurredAt
) {
}
