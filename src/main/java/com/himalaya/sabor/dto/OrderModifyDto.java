package com.himalaya.sabor.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO para la modificación de un pedido dentro de la ventana de 10 minutos (US12 / P11).
 */
public class OrderModifyDto {

    @NotBlank(message = "La dirección de entrega es obligatoria")
    private String deliveryAddress;

    @NotBlank(message = "El teléfono de contacto es obligatorio")
    private String phone;

    private String notes;

    private List<OrderItemModifyDto> items = new ArrayList<>();

    private Long addProductId;
    private Integer addQuantity = 1;

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<OrderItemModifyDto> getItems() {
        return items;
    }

    public void setItems(List<OrderItemModifyDto> items) {
        this.items = items;
    }

    public Long getAddProductId() {
        return addProductId;
    }

    public void setAddProductId(Long addProductId) {
        this.addProductId = addProductId;
    }

    public Integer getAddQuantity() {
        return addQuantity;
    }

    public void setAddQuantity(Integer addQuantity) {
        this.addQuantity = addQuantity;
    }

    public static class OrderItemModifyDto {
        private Long orderItemId;
        private Integer quantity;

        public OrderItemModifyDto() {
        }

        public OrderItemModifyDto(Long orderItemId, Integer quantity) {
            this.orderItemId = orderItemId;
            this.quantity = quantity;
        }

        public Long getOrderItemId() {
            return orderItemId;
        }

        public void setOrderItemId(Long orderItemId) {
            this.orderItemId = orderItemId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }
}
