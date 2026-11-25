package com.himalaya.sabor.model;

public enum OrderStatus {
    RECIBIDO("Recibido"),
    EN_PREPARACION("En preparación"),
    EN_REPARTO("En reparto"),
    ENTREGADO("Entregado"),
    CANCELADO("Cancelado");

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
