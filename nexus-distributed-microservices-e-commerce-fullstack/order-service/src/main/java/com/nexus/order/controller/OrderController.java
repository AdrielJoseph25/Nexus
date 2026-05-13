package com.nexus.order.controller;

import com.nexus.common.dto.CreateOrderRequest;
import com.nexus.order.domain.CustomerOrder;
import com.nexus.order.service.OrderSagaService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderSagaService orderSagaService;

    public OrderController(OrderSagaService orderSagaService) {
        this.orderSagaService = orderSagaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CustomerOrder create(@RequestBody CreateOrderRequest request) {
        return orderSagaService.createOrder(request);
    }

    @GetMapping
    public List<CustomerOrder> list() {
        return orderSagaService.listOrders();
    }
}
