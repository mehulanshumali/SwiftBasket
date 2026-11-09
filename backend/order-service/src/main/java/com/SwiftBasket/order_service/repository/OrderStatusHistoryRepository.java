package com.SwiftBasket.order_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.SwiftBasket.order_service.entity.OrderStatusHistory;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {
    List<OrderStatusHistory> findByOrderIdOrderByChangedAtAsc(Long orderId);
}
