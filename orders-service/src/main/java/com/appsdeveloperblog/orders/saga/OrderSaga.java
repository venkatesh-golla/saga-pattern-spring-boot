package com.appsdeveloperblog.orders.saga;

import com.appsdeveloperblog.core.dto.commands.ReserveProductCommand;
import com.appsdeveloperblog.core.dto.events.OrderCreatedEvent;
import com.appsdeveloperblog.core.types.OrderStatus;
import com.appsdeveloperblog.orders.service.OrderHistoryService;
import com.appsdeveloperblog.orders.service.OrderHistoryServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@KafkaListener(topics = "${orders.events.topic.name}")
public class OrderSaga {
  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final OrderHistoryService orderHistoryService;
  private final String productsCommandTopicName;

  public OrderSaga(
      KafkaTemplate<String, Object> kafkaTemplate,
      OrderHistoryService orderHistoryService,
      @Value("${products.commands.topic.name}") String productsCommandTopicName) {
    this.kafkaTemplate = kafkaTemplate;
    this.orderHistoryService = orderHistoryService;
    this.productsCommandTopicName = productsCommandTopicName;
  }

  @KafkaHandler
  public void handleEvent(@Payload OrderCreatedEvent orderCreatedEvent) {
    ReserveProductCommand reserveProductCommand =
        new ReserveProductCommand(
            orderCreatedEvent.getOrderId(),
            orderCreatedEvent.getProductId(),
            orderCreatedEvent.getProductQuantity());

    kafkaTemplate.send(productsCommandTopicName, reserveProductCommand);
    orderHistoryService.add(orderCreatedEvent.getOrderId(), OrderStatus.CREATED);
  }
}
