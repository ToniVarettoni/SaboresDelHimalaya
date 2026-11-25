package com.himalaya.sabor.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = true)
    private Product product;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "daily_menu_id", nullable = true)
    private DailyMenu dailyMenu;

    @Column(columnDefinition = "TEXT")
    private String itemDetailsJson;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    public OrderItem() {
    }

    public OrderItem(Order order, Product product, BigDecimal unitPrice, Integer quantity, BigDecimal subtotal) {
        this.order = order;
        this.product = product;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.subtotal = subtotal;
    }

    public OrderItem(Order order, DailyMenu dailyMenu, String itemDetailsJson, BigDecimal unitPrice, Integer quantity, BigDecimal subtotal) {
        this.order = order;
        this.dailyMenu = dailyMenu;
        this.itemDetailsJson = itemDetailsJson;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.subtotal = subtotal;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public DailyMenu getDailyMenu() {
        return dailyMenu;
    }

    public void setDailyMenu(DailyMenu dailyMenu) {
        this.dailyMenu = dailyMenu;
    }

    public String getItemDetailsJson() {
        return itemDetailsJson;
    }

    public void setItemDetailsJson(String itemDetailsJson) {
        this.itemDetailsJson = itemDetailsJson;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    /**
     * Nombre legible del ítem (producto de carta o menú diario).
     */
    @Transient
    public String getDisplayName() {
        if (product != null) {
            return product.getName();
        }
        String extracted = extractJsonField("name");
        return extracted != null ? extracted : "Menú Diario";
    }

    /**
     * Detalle de platos elegidos en caso de ser un menú diario.
     */
    @Transient
    public String getDisplayDetails() {
        return extractJsonField("platos");
    }

    private String extractJsonField(String field) {
        if (itemDetailsJson == null || itemDetailsJson.isBlank()) {
            return null;
        }
        String token = "\"" + field + "\":\"";
        int start = itemDetailsJson.indexOf(token);
        if (start < 0) {
            return null;
        }
        int valStart = start + token.length();
        int valEnd = itemDetailsJson.indexOf("\"", valStart);
        if (valEnd < 0) {
            return null;
        }
        return itemDetailsJson.substring(valStart, valEnd);
    }
}
