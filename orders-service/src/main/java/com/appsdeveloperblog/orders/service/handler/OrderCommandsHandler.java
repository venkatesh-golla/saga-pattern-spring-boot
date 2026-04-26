package com.appsdeveloperblog.orders.service.handler;

import com.appsdeveloperblog.core.dto.commands.ApprovedOrderCommand;
import com.appsdeveloperblog.orders.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@KafkaListener(topics = "${orders.commands.topic.name}")
public class OrderCommandsHandler {
  private final OrderService orderService;
  private final Logger logger = LoggerFactory.getLogger(OrderCommandsHandler.class);

  public OrderCommandsHandler(OrderService orderService) {
    this.orderService = orderService;
  }

  @KafkaHandler
  public void handleCommand(@Payload ApprovedOrderCommand command) {
    try {
      logger.info("Received ApprovedOrderCommand for order with id {}", command.getOrderId());
      orderService.approveOrder(command.getOrderId());
      logger.info("Order approved for order with id {}", command.getOrderId());
    } catch (Exception e) {
      logger.info(
          "Error approving order with id {}: {}", command.getOrderId(), e.getLocalizedMessage(), e);
      throw new RuntimeException(e);
    }
  }
}
