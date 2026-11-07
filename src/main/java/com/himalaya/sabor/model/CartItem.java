package com.himalaya.sabor.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Representa un ítem dentro del carrito de compra. No es una entidad JPA porque
 * su vida está limitada a la sesión HTTP del cliente.
 */
public class CartItem {
    /**
     * Identificador interno del ítem (único dentro del carrito). Se genera al
     * crear el ítem para poder referenciarlo en operaciones de actualización o
     * borrado.
     */
    private final String id = UUID.randomUUID().toString();

    /** Tipo de ítem: producto de la carta o ítem del menú diario */
    public enum ItemType { PRODUCT, DAILY_MENU }

    private ItemType type;
    private Long productId;          // nullable cuando type == DAILY_MENU
    private Long dailyMenuItemId;    // nullable cuando type == PRODUCT
    private String name;            // nombre a mostrar (producto o customName)
    private BigDecimal unitPrice;
    private int quantity = 1;

    public CartItem() {}

    public CartItem(ItemType type, Long productId, Long dailyMenuItemId,
                    String name, BigDecimal unitPrice, int quantity) {
        this.type = type;
        this.productId = productId;
        this.dailyMenuItemId = dailyMenuItemId;
        this.name = name;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public String getId() { return id; }
    public ItemType getType() { return type; }
    public void setType(ItemType type) { this.type = type; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getDailyMenuItemId() { return dailyMenuItemId; }
    public void setDailyMenuItemId(Long dailyMenuItemId) { this.dailyMenuItemId = dailyMenuItemId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
