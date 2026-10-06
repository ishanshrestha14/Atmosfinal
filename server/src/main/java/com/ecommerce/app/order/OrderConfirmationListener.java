package com.ecommerce.app.order;

import com.ecommerce.app.email.EmailService;
import com.ecommerce.app.email.EmailTemplateName;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends the confirmation email only once the order is durably committed, so customers are never
 * emailed about an order that was rolled back. Delivery is asynchronous and best effort: a mail
 * failure is logged and never affects the order.
 */
@Component
@RequiredArgsConstructor
public class OrderConfirmationListener {

    private final EmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPlaced(OrderPlacedEvent event) throws MessagingException {
        emailService.sendOrderEmail(event.customerEmail(), event.customerUsername(),
                EmailTemplateName.ORDER_CONFIRMATION, event.items(), event.totalPrice(), "Your Order Confirmation");
    }
}
