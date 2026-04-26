package com.appsdeveloperblog.core.dto.events;

import java.util.UUID;

public class ProductReservationFailedEvent {
  private UUID productId;
  private UUID orderId;
  private Integer quantity;

  public ProductReservationFailedEvent() {}

  public ProductReservationFailedEvent(UUID productId, UUID orderId, Integer quantity) {
    this.productId = productId;
    this.orderId = orderId;
    this.quantity = quantity;
  }

  public Integer getQuantity() {
    return quantity;
  }

  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }

  public UUID getOrderId() {
    return orderId;
  }

  public void setOrderId(UUID orderId) {
    this.orderId = orderId;
  }

  public UUID getProductId() {
    return productId;
  }

  public void setProductId(UUID productId) {
    this.productId = productId;
  }
}
