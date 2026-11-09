package com.SwiftBasket.payment_service.service;

import com.SwiftBasket.payment_service.entity.Payment;

public interface PaymentService {
    Payment createIntent(Long userId, Long orderId);
    Payment confirm(Long userId, Long orderId, String paymentMethodId);
    Payment getByOrderId(Long userId, Long orderId);
    Payment getReceiptPayment(Long userId, Long orderId);
    void handleWebhookEvent(String payload, String signatureHeader);
}
