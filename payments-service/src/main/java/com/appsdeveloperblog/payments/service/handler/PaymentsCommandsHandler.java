package com.appsdeveloperblog.payments.service.handler;

import com.appsdeveloperblog.core.dto.Payment;
import com.appsdeveloperblog.core.dto.commands.ProcessPaymentCommand;
import com.appsdeveloperblog.core.dto.events.PaymentFailedEvent;
import com.appsdeveloperblog.core.dto.events.PaymentProcessedEvent;
import com.appsdeveloperblog.core.exceptions.CreditCardProcessorUnavailableException;
import com.appsdeveloperblog.payments.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@KafkaListener(topics = "${payments.commands.topic.name}")
public class PaymentsCommandsHandler {
  private final PaymentService paymentService;
  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final String paymentsEventsTopicName;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  public PaymentsCommandsHandler(
      PaymentService paymentService,
      KafkaTemplate<String, Object> kafkaTemplate,
      @Value("${payments.events.topic.name}") String paymentsEventsTopicName) {
    this.paymentService = paymentService;
    this.kafkaTemplate = kafkaTemplate;
    this.paymentsEventsTopicName = paymentsEventsTopicName;
  }

  @KafkaHandler
  public void handlePaymentsCommand(@Payload ProcessPaymentCommand processPaymentCommand) {
    try {
      Payment payment =
          new Payment(
              processPaymentCommand.getOrderId(),
              processPaymentCommand.getProductId(),
              processPaymentCommand.getProductPrice(),
              processPaymentCommand.getProductQuantity());
      paymentService.process(payment);

      PaymentProcessedEvent paymentProcessedEvent =
          new PaymentProcessedEvent(payment.getOrderId(), payment.getId());
      kafkaTemplate.send(paymentsEventsTopicName, paymentProcessedEvent);

    } catch (CreditCardProcessorUnavailableException e) {
      logger.error(e.getLocalizedMessage(), e);
      PaymentFailedEvent paymentFailedEvent =
          new PaymentFailedEvent(
              processPaymentCommand.getOrderId(),
              processPaymentCommand.getProductId(),
              processPaymentCommand.getProductQuantity());
      kafkaTemplate.send(paymentsEventsTopicName, paymentFailedEvent);
    }
  }
}
