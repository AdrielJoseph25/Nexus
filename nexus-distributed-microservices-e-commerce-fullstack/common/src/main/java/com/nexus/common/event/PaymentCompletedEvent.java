package com.nexus.common.event;

import java.time.Instant;
import java.util.UUID;

public record PaymentCompletedEvent(
        UUID orderId,
        UUID paymentId,
        Instant occurredAt
) {
}
