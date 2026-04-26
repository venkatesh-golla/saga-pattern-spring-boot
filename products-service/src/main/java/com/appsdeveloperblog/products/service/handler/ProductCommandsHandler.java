package com.appsdeveloperblog.products.service.handler;

import com.appsdeveloperblog.core.dto.Product;
import com.appsdeveloperblog.core.dto.commands.ReserveProductCommand;
import com.appsdeveloperblog.core.dto.events.ProductReservationFailedEvent;
import com.appsdeveloperblog.core.dto.events.ProductReservedEvent;
import com.appsdeveloperblog.products.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@KafkaListener(topics = "${products.commands.topic.name}")
public class ProductCommandsHandler {
  private final ProductService productService;
  private final Logger logger = LoggerFactory.getLogger(ProductCommandsHandler.class);
  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final String productEventsTopicName;

  public ProductCommandsHandler(
      ProductService productService,
      KafkaTemplate<String, Object> kafkaTemplate,
      @Value("products.events.topic.name") String productEventsTopicName) {
    this.productService = productService;
    this.kafkaTemplate = kafkaTemplate;
    this.productEventsTopicName = productEventsTopicName;
  }

  @KafkaHandler
  public void handleCommand(@Payload ReserveProductCommand reserveProductCommand) {

    try {
      Product reservedProduct =
          new Product(
              reserveProductCommand.getProductId(), reserveProductCommand.getProductQuantity());
      productService.reserve(reservedProduct, reserveProductCommand.getOrderId());

      ProductReservedEvent productReservedEvent =
          new ProductReservedEvent(
              reserveProductCommand.getOrderId(),
              reserveProductCommand.getProductId(),
              reservedProduct.getPrice(),
              reserveProductCommand.getProductQuantity());
      kafkaTemplate.send(productEventsTopicName, productReservedEvent);
    } catch (Exception e) {
      logger.error(
          "Failed to reserve product with id {} for order with id {}. Reason: {}",
          reserveProductCommand.getProductId(),
          reserveProductCommand.getOrderId(),
          e.getMessage());
    }
    ProductReservationFailedEvent productReservationFailedEvent =
        new ProductReservationFailedEvent(
            reserveProductCommand.getOrderId(),
            reserveProductCommand.getProductId(),
            reserveProductCommand.getProductQuantity());
    kafkaTemplate.send(productEventsTopicName, productReservationFailedEvent);
  }
}
