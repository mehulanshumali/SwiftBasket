package com.SwiftBasket.notification_service.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

import com.SwiftBasket.notification_service.event.OrderPlacedEvent.EventItem;

import lombok.extern.slf4j.Slf4j;

/**
 * Inline HTML email templates - no template engine dependency on purpose:
 * four simple emails do not justify Thymeleaf/Freemarker.
 */
@Component
@Slf4j
public class EmailTemplates {

    private static final String WRAPPER_OPEN =
            "<div style='font-family:Arial,Helvetica,sans-serif;max-width:600px;margin:0 auto;"
            + "border:1px solid #e0e0e0;border-radius:8px;overflow:hidden'>"
            + "<div style='background:#4F46E5;color:#fff;padding:20px;text-align:center'>"
            + "<h1 style='margin:0'>SwiftBasket</h1></div>"
            + "<div style='padding:24px;color:#333'>";

    private static final String WRAPPER_CLOSE = "</div><div style='background:#f9fafb;padding:16px;"
            + "text-align:center;color:#9ca3af;font-size:12px'>SwiftBasket - Bengaluru, India</div></div>";

    public String welcome(String fullName) {
        return WRAPPER_OPEN
                + "<h2>Welcome, " + escape(fullName) + "!</h2>"
                + "<p>Your SwiftBasket account is ready. Browse the catalog, fill your cart, "
                + "and check out in seconds.</p>"
                + WRAPPER_CLOSE;
    }

    public String orderConfirmation(Long orderId, List<EventItem> items, BigDecimal total, String currency) {
        StringBuilder rows = new StringBuilder();
        for (EventItem item : items) {
            BigDecimal lineTotal = item.getUnitPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
            rows.append("<tr><td style='padding:8px 0'>").append(escape(item.getProductName()))
                    .append(" x ").append(item.getQuantity())
                    .append("</td><td style='padding:8px 0;text-align:right'>")
                    .append(currency).append(" ").append(lineTotal)
                    .append("</td></tr>");
        }
        return WRAPPER_OPEN
                + "<h2>Order #" + orderId + " confirmed</h2>"
                + "<p>Thanks for your order! Here is what you bought:</p>"
                + "<table style='width:100%;border-collapse:collapse'>"
                + rows
                + "<tr><td style='padding:12px 0;border-top:2px solid #4F46E5;font-weight:bold'>Total</td>"
                + "<td style='padding:12px 0;border-top:2px solid #4F46E5;text-align:right;font-weight:bold'>"
                + currency + " " + total
                + "</td></tr></table>"
                + "<p>We are preparing your order. You will receive another email once it ships.</p>"
                + WRAPPER_CLOSE;
    }

    public String paymentReceipt(Long paymentId, Long orderId, BigDecimal amount, String currency) {
        return WRAPPER_OPEN
                + "<h2>Payment receipt</h2>"
                + "<p>We received your payment for order <b>#" + orderId + "</b>.</p>"
                + "<table style='width:100%'>"
                + "<tr><td style='padding:4px 0'>Receipt number</td>"
                + "<td style='padding:4px 0;text-align:right'>R-" + paymentId + "</td></tr>"
                + "<tr><td style='padding:4px 0'>Amount paid</td>"
                + "<td style='padding:4px 0;text-align:right'>" + currency + " " + amount + "</td></tr>"
                + "</table>"
                + "<p>Keep this receipt for your records.</p>"
                + WRAPPER_CLOSE;
    }

    public String statusUpdate(Long orderId, String toStatus, String note) {
        String body = "<h2>Order #" + orderId + " update</h2>"
                + "<p>Your order status changed to <b>" + escape(toStatus) + "</b>.</p>";
        if (note != null && !note.isBlank()) {
            body += "<p style='background:#f3f4f6;padding:12px;border-radius:6px'>" + escape(note) + "</p>";
        }
        return WRAPPER_OPEN + body + WRAPPER_CLOSE;
    }

    // minimal HTML escaping for user-provided strings (names, notes, product names)
    private String escape(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
