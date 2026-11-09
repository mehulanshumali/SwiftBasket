package com.SwiftBasket.payment_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.SwiftBasket.payment_service.entity.PaymentEventLog;

public interface PaymentEventLogRepository extends JpaRepository<PaymentEventLog, Long> {
    List<PaymentEventLog> findByPaymentIdOrderByCreatedAtAsc(Long paymentId);
}
