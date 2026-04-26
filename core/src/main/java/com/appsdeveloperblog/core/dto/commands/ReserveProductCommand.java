package com.appsdeveloperblog.core.dto.commands;

import java.util.UUID;

public class ReserveProductCommand {
  private UUID orderId;
  private UUID productId;
  private Integer productQuantity;

  public ReserveProductCommand() {}

  public ReserveProductCommand(UUID orderId, UUID productId, Integer productQuantity) {
    this.orderId = orderId;
    this.productId = productId;
    this.productQuantity = productQuantity;
  }

  public Integer getProductQuantity() {
    return productQuantity;
  }

  public void setProductQuantity(Integer productQuantity) {
    this.productQuantity = productQuantity;
  }

  public UUID getProductId() {
    return productId;
  }

  public void setProductId(UUID productId) {
    this.productId = productId;
  }

  public UUID getOrderId() {
    return orderId;
  }

  public void setOrderId(UUID orderId) {
    this.orderId = orderId;
  }
}
