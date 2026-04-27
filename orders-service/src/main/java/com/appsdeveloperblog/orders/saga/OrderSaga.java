package com.appsdeveloperblog.orders.saga;

import com.appsdeveloperblog.core.dto.commands.*;
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
      @Value("${orders.commands.topic.name}") String orderCommandsTopicName) {
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
    try {
      log.info("Sending ApprovedOrderCommand for orderId: {}", approvedOrderCommand.getOrderId());
      kafkaTemplate.send(orderCommandsTopicName, approvedOrderCommand);
      log.info("ApprovedOrderCommand sent for orderId: {}", approvedOrderCommand.getOrderId());
    } catch (Exception e) {
      log.error(
          "Failed to send ApprovedOrderCommand for orderId: {}. Error: {}",
          approvedOrderCommand.getOrderId(),
          e.getMessage(),
          e);
      throw new RuntimeException(e);
    }
  }

  @KafkaHandler
  public void handleEvent(@Payload OrderApprovedEvent orderApprovedEvent) {
    try {
      log.info("Received OrderApprovedEvent for orderId: {}", orderApprovedEvent.getOrderId());
      orderHistoryService.add(orderApprovedEvent.getOrderId(), OrderStatus.APPROVED);
      log.info(
          "Order history updated to APPROVED for orderId: {}", orderApprovedEvent.getOrderId());
    } catch (Exception e) {
      log.error(
          "Failed to update order history for orderId: {}. Error: {}",
          orderApprovedEvent.getOrderId(),
          e.getMessage(),
          e);
      throw new RuntimeException(e);
    }
  }

  @KafkaHandler
  public void handleEvent(@Payload PaymentFailedEvent paymentFailedEvent) {
    CancelProductReservationCommand cancelProductReservationCommand =
        new CancelProductReservationCommand(
            paymentFailedEvent.getProductId(),
            paymentFailedEvent.getOrderId(),
            paymentFailedEvent.getProductQuantity());
    try {
      log.info(
          "Sending CancelProductReservationCommand for : productId {}, orderId: {}, quantity: {}",
          cancelProductReservationCommand.getProductId(),
          cancelProductReservationCommand.getOrderId(),
          cancelProductReservationCommand.getProductQuantity());
      kafkaTemplate.send(productsCommandTopicName, cancelProductReservationCommand);
      log.info(
          "CancelProductReservationCommand sent for orderId: {}, productId: {}, quantity: {}",
          cancelProductReservationCommand.getOrderId(),
          cancelProductReservationCommand.getProductId(),
          cancelProductReservationCommand.getProductQuantity());
    } catch (Exception e) {
      log.error(
          "Failed to send CancelProductReservationCommand for orderId: {}, productId: {}, quantity: {}. Error: {}",
          cancelProductReservationCommand.getOrderId(),
          cancelProductReservationCommand.getProductId(),
          cancelProductReservationCommand.getProductQuantity(),
          e.getMessage(),
          e);
      throw new RuntimeException(e);
    }
  }

  @KafkaHandler
  public void handleEvent(@Payload ProductReservationCancelledEvent event) {
    RejectOrderCommand rejectOrderCommand = new RejectOrderCommand(event.getOrderId());
    kafkaTemplate.send(orderCommandsTopicName, rejectOrderCommand);
    orderHistoryService.add(event.getOrderId(), OrderStatus.REJECTED);
  }
}
