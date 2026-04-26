package com.appsdeveloperblog.orders.saga;

import com.appsdeveloperblog.core.dto.commands.ApprovedOrderCommand;
import com.appsdeveloperblog.core.dto.commands.ProcessPaymentCommand;
import com.appsdeveloperblog.core.dto.commands.ReserveProductCommand;
import com.appsdeveloperblog.core.dto.events.*;
import com.appsdeveloperblog.core.types.OrderStatus;
import com.appsdeveloperblog.orders.service.OrderHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@KafkaListener(
    topics = {
      "${orders.events.topic.name}",
      "${products.events.topic.name}",
      "${payments.events.topic.name}"
    })
public class OrderSaga {
  private static final Logger log = LoggerFactory.getLogger(OrderSaga.class);
  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final OrderHistoryService orderHistoryService;
  private final String productsCommandTopicName;
  private final String paymentsCommandTopicName;
  private final String orderCommandsTopicName;

  public OrderSaga(
      KafkaTemplate<String, Object> kafkaTemplate,
      OrderHistoryService orderHistoryService,
      @Value("${products.commands.topic.name}") String productsCommandTopicName,
      @Value("${payments.commands.topic.name}") String paymentsCommandTopicName,
      @Value("${orders.commands.topic.name") String orderCommandsTopicName) {
    this.kafkaTemplate = kafkaTemplate;
    this.orderHistoryService = orderHistoryService;
    this.productsCommandTopicName = productsCommandTopicName;
    this.paymentsCommandTopicName = paymentsCommandTopicName;
    this.orderCommandsTopicName = orderCommandsTopicName;
  }

  @KafkaHandler
  public void handleEvent(@Payload OrderCreatedEvent orderCreatedEvent) {
    ReserveProductCommand reserveProductCommand =
        new ReserveProductCommand(
            orderCreatedEvent.getProductId(),
            orderCreatedEvent.getProductQuantity(),
            orderCreatedEvent.getOrderId());

    kafkaTemplate.send(productsCommandTopicName, reserveProductCommand);
    orderHistoryService.add(orderCreatedEvent.getOrderId(), OrderStatus.CREATED);
  }

  @KafkaHandler
  public void handleEvent(@Payload ProductReservedEvent productReservedEvent) {
    ProcessPaymentCommand processPaymentCommand =
        new ProcessPaymentCommand(
            productReservedEvent.getOrderId(),
            productReservedEvent.getProductId(),
            productReservedEvent.getProductPrice(),
            productReservedEvent.getProductQuantity());
    try {
      log.info(
          "Sending ProcessPaymentCommand for orderId: {}, productId: {}, price: {}, quantity: {}",
          processPaymentCommand.getOrderId(),
          processPaymentCommand.getProductId(),
          processPaymentCommand.getProductPrice(),
          processPaymentCommand.getProductQuantity());
      kafkaTemplate.send(paymentsCommandTopicName, processPaymentCommand);
      log.info(
          "ProcessPaymentCommand sent for orderId: {}, productId: {}, price: {}, quantity: {}",
          processPaymentCommand.getOrderId(),
          processPaymentCommand.getProductId(),
          processPaymentCommand.getProductPrice(),
          processPaymentCommand.getProductQuantity());
    } catch (Exception e) {
      log.error(
          "Failed to send ProcessPaymentCommand for orderId: {}, productId: {}, price: {}, quantity: {}. Error: {}",
          processPaymentCommand.getOrderId(),
          processPaymentCommand.getProductId(),
          processPaymentCommand.getProductPrice(),
          processPaymentCommand.getProductQuantity(),
          e.getMessage(),
          e);
      throw new RuntimeException(e);
    }
  }

  @KafkaHandler
  public void handleEvent(@Payload PaymentProcessedEvent paymentProcessedEvent) {
    ApprovedOrderCommand approvedOrderCommand =
        new ApprovedOrderCommand(paymentProcessedEvent.getOrderId());
    kafkaTemplate.send(orderCommandsTopicName, approvedOrderCommand);
  }

  @KafkaHandler
  public void handleEvent(@Payload OrderApprovedEvent orderApprovedEvent) {
    orderHistoryService.add(orderApprovedEvent.getOrderId(), OrderStatus.APPROVED);
  }
}
