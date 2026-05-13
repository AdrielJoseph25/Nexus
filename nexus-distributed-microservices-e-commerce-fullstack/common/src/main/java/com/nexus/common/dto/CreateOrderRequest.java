package com.nexus.common.dto;

import java.math.BigDecimal;

public record CreateOrderRequest(
        String customerId,
        String sku,
        int quantity,
        BigDecimal amount
) {
}
