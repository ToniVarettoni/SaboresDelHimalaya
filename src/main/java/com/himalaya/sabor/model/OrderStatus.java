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

    /** Siguiente estado del flujo del restaurante (US14, CA14.1); null si el pedido ya está finalizado. */
    public OrderStatus next() {
        return switch (this) {
            case RECIBIDO -> EN_PREPARACION;
            case EN_PREPARACION -> EN_REPARTO;
            case EN_REPARTO -> ENTREGADO;
            default -> null;
        };
    }
}
