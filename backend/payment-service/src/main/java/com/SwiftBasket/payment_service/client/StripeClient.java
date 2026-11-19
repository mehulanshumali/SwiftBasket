package com.SwiftBasket.payment_service.client;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.SwiftBasket.payment_service.exception.StripeApiException;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentConfirmParams;
import com.stripe.param.PaymentIntentCreateParams;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class StripeClient {

    @Value("${stripe.api-key}")
    private String apiKey;

    @Value("${stripe.test-payment-method-id:${STRIPE_TEST_PAYMENT_METHOD_ID:}}")
    private String testPaymentMethodId;

    @PostConstruct
    public void init() {
        // fail-fast: bad key config should surface at boot, not at first payment
        Stripe.apiKey = apiKey;
        log.info("Stripe client initialized");
    }

    public String getTestPaymentMethodId() {
        boolean testMode = apiKey.startsWith("sk_test_") || apiKey.startsWith("rk_test_");
        return testMode && testPaymentMethodId.startsWith("pm_") ? testPaymentMethodId : null;
    }

    public PaymentIntent createPaymentIntent(BigDecimal amount, String currency) {
        log.info("Creating Stripe PaymentIntent - amount: {} {}", currency, amount);
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(toStripeAmount(amount))
                    .setCurrency(currency.toLowerCase())
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build())
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);
            log.info("PaymentIntent created - id: {}, status: {}", intent.getId(), intent.getStatus());
            return intent;

        } catch (StripeException e) {
            log.error("Stripe PaymentIntent creation failed: {}", e.getMessage());
            throw new StripeApiException("Payment gateway error while creating payment intent");
        }
    }

    // confirms an existing intent with the customer's payment method (test tokens)
    public PaymentIntent confirmIntent(String intentId, String paymentMethodId) {
        log.info("Confirming Stripe PaymentIntent {} with method {}", intentId, paymentMethodId);
        try {
            PaymentIntent intent = PaymentIntent.retrieve(intentId);

            PaymentIntentConfirmParams params = PaymentIntentConfirmParams.builder()
                    .setPaymentMethod(paymentMethodId)
                    .build();

            PaymentIntent confirmed = intent.confirm(params);
            log.info("PaymentIntent confirmed - id: {}, status: {}", confirmed.getId(), confirmed.getStatus());
            return confirmed;

        } catch (StripeException e) {
            log.error("Stripe PaymentIntent confirmation failed for {}: {}", intentId, e.getMessage());
            throw new StripeApiException("Payment gateway error while confirming payment");
        }
    }

    public PaymentIntent retrieveIntent(String intentId) {
        try {
            return PaymentIntent.retrieve(intentId);
        } catch (StripeException e) {
            log.error("Stripe PaymentIntent retrieval failed for {}: {}", intentId, e.getMessage());
            throw new StripeApiException("Payment gateway error while retrieving payment");
        }
    }

    // Stripe amounts are minor units (paise for INR) - never floats
    private long toStripeAmount(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).setScale(0, java.math.RoundingMode.HALF_UP).longValueExact();
    }
}
