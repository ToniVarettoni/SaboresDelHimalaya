package com.himalaya.sabor;

import com.himalaya.sabor.model.Order;
import com.himalaya.sabor.model.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** Pruebas unitarias (sin Spring) de las reglas de pedidos del Sprint 2. */
class OrderRulesUnitTests {

    private Order pedidoCreadoHace(int minutos, OrderStatus estado) {
        Order o = new Order();
        o.setCreatedAt(LocalDateTime.now().minusMinutes(minutos));
        o.setStatus(estado);
        return o;
    }

    @Test
    @DisplayName("US14 (CA14.1): el estado avanza Recibido → En preparación → En reparto → Entregado")
    void orderStatusAdvancesSequentially() {
        assertThat(OrderStatus.RECIBIDO.next()).isEqualTo(OrderStatus.EN_PREPARACION);
        assertThat(OrderStatus.EN_PREPARACION.next()).isEqualTo(OrderStatus.EN_REPARTO);
        assertThat(OrderStatus.EN_REPARTO.next()).isEqualTo(OrderStatus.ENTREGADO);
        assertThat(OrderStatus.ENTREGADO.next()).isNull();
        assertThat(OrderStatus.CANCELADO.next()).isNull();
    }

    @Test
    @DisplayName("US12 y US13 (CA12.1): un pedido de hace 9 minutos está dentro del plazo y uno de hace 11 no")
    void tenMinuteWindow() {
        assertThat(pedidoCreadoHace(9, OrderStatus.RECIBIDO).isWithinTenMinutes()).isTrue();
        assertThat(pedidoCreadoHace(11, OrderStatus.RECIBIDO).isWithinTenMinutes()).isFalse();
    }

    @Test
    @DisplayName("US12 y US13: un pedido cancelado o entregado no se puede modificar aunque esté dentro del plazo")
    void cancelledOrDeliveredOrderIsNotModifiable() {
        assertThat(pedidoCreadoHace(2, OrderStatus.RECIBIDO).isModifiable()).isTrue();
        assertThat(pedidoCreadoHace(2, OrderStatus.CANCELADO).isModifiable()).isFalse();
        assertThat(pedidoCreadoHace(2, OrderStatus.ENTREGADO).isModifiable()).isFalse();
    }
}
