package com.himalaya.sabor.dto;

import com.himalaya.sabor.model.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para el formulario de confirmación de pedido (P8 del flujo de pantallas).
 */
public class OrderConfirmDto {

    @NotBlank(message = "La dirección de entrega es obligatoria")
    private String deliveryAddress;

    @NotBlank(message = "El teléfono de contacto es obligatorio")
    private String phone;

    @NotNull(message = "Selecciona un método de pago")
    private PaymentMethod paymentMethod;

    private String notes;

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
