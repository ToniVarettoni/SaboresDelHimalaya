package com.himalaya.sabor;

import com.himalaya.sabor.model.*;
import com.himalaya.sabor.repository.OrderRepository;
import com.himalaya.sabor.repository.ProductRepository;
import com.himalaya.sabor.repository.UserRepository;
import com.himalaya.sabor.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ClientOrderIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private User cliente;
    private Product plato1;
    private Product plato2;

    @BeforeEach
    void setUp() {
        cliente = userRepository.findByEmail("cliente.pedidos@himalaya.com").orElseGet(() -> {
            User u = new User("Cliente Pedidos", "cliente.pedidos@himalaya.com",
                    passwordEncoder.encode("pass123"), "Calle Sol 1", "600123456",
                    Role.CLIENT, UserStatus.ACTIVA);
            return userRepository.save(u);
        });
        var productos = productRepository.findAll();
        plato1 = productos.get(0);
        plato2 = productos.get(1);
    }

    private Order crearPedido(OrderStatus status, LocalDateTime createdAt, int cantidad) {
        Order o = new Order();
        o.setOrderNumber("PED-TEST-" + System.nanoTime());
        o.setUser(cliente);
        o.setDeliveryAddress("Calle Sol 1");
        o.setPhone("600123456");
        o.setPaymentMethod(PaymentMethod.EFECTIVO);
        o.setStatus(status);
        o.setCreatedAt(createdAt);

        BigDecimal sub = plato1.getPrice().multiply(BigDecimal.valueOf(cantidad));
        o.setTotalAmount(sub);
        o.addItem(new OrderItem(o, plato1, plato1.getPrice(), cantidad, sub));
        return orderRepository.save(o);
    }

    @Test
    @WithMockUser(username = "cliente.pedidos@himalaya.com", roles = {"CLIENT"})
    @DisplayName("US11 (CA11.1 y CA11.2): muestra todo el historial de pedidos (activos, entregados y cancelados) y su detalle")
    void clientViewsFullHistoryAndDetail() throws Exception {
        Order activo = crearPedido(OrderStatus.RECIBIDO, LocalDateTime.now().minusMinutes(2), 1);
        Order entregado = crearPedido(OrderStatus.ENTREGADO, LocalDateTime.now().minusDays(1), 2);
        Order cancelado = crearPedido(OrderStatus.CANCELADO, LocalDateTime.now().minusDays(2), 1);

        mockMvc.perform(get("/mis-pedidos"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(activo.getOrderNumber())))
                .andExpect(content().string(containsString(entregado.getOrderNumber())))
                .andExpect(content().string(containsString(cancelado.getOrderNumber())))
                .andExpect(content().string(containsString("Recibido")))
                .andExpect(content().string(containsString("Entregado")))
                .andExpect(content().string(containsString("Cancelado")));

        mockMvc.perform(get("/mis-pedidos/" + entregado.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(plato1.getName())))
                .andExpect(content().string(containsString("Calle Sol 1")));
    }

    @Test
    @WithMockUser(username = "cliente.pedidos@himalaya.com", roles = {"CLIENT"})
    @DisplayName("US12 (CA12.2): modifica cantidad, añade producto y dirección dentro de los 10 minutos recalculando el total")
    void clientModifiesOrderWithinTenMinutes() throws Exception {
        Order order = crearPedido(OrderStatus.RECIBIDO, LocalDateTime.now().minusMinutes(3), 1);
        Long itemId = order.getItems().get(0).getId();

        mockMvc.perform(post("/mis-pedidos/" + order.getId() + "/modificar")
                        .with(csrf())
                        .param("deliveryAddress", "Avenida Nueva 99")
                        .param("phone", "699888777")
                        .param("notes", "Sin picante")
                        .param("items[0].orderItemId", itemId.toString())
                        .param("items[0].quantity", "3")
                        .param("addProductId", plato2.getId().toString())
                        .param("addQuantity", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mis-pedidos/" + order.getId()));

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        BigDecimal expectedTotal = plato1.getPrice().multiply(BigDecimal.valueOf(3)).add(plato2.getPrice());
        assertThat(updated.getDeliveryAddress()).isEqualTo("Avenida Nueva 99");
        assertThat(updated.getPhone()).isEqualTo("699888777");
        assertThat(updated.getItems()).hasSize(2);
        assertThat(updated.getTotalAmount()).isEqualByComparingTo(expectedTotal);
    }

    @Test
    @WithMockUser(username = "cliente.pedidos@himalaya.com", roles = {"CLIENT"})
    @DisplayName("US12 (CA12.3): rechaza modificación pasados 10 minutos con el mensaje exacto")
    void clientCannotModifyOrderAfterTenMinutes() throws Exception {
        Order oldOrder = crearPedido(OrderStatus.EN_PREPARACION, LocalDateTime.now().minusMinutes(11), 1);
        Long itemId = oldOrder.getItems().get(0).getId();

        mockMvc.perform(get("/mis-pedidos/" + oldOrder.getId() + "/modificar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMsg", OrderService.MSG_MODIFICAR_EXPIRADO));

        mockMvc.perform(post("/mis-pedidos/" + oldOrder.getId() + "/modificar")
                        .with(csrf())
                        .param("deliveryAddress", "Otra Calle")
                        .param("phone", "600000000")
                        .param("items[0].orderItemId", itemId.toString())
                        .param("items[0].quantity", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMsg", OrderService.MSG_MODIFICAR_EXPIRADO));

        Order unchanged = orderRepository.findById(oldOrder.getId()).orElseThrow();
        assertThat(unchanged.getDeliveryAddress()).isEqualTo("Calle Sol 1");
    }

    @Test
    @WithMockUser(username = "cliente.pedidos@himalaya.com", roles = {"CLIENT"})
    @DisplayName("US13 (CA13.1): cancela pedido dentro de los 10 minutos sin coste")
    void clientCancelsOrderWithinTenMinutes() throws Exception {
        Order order = crearPedido(OrderStatus.RECIBIDO, LocalDateTime.now().minusMinutes(5), 1);

        mockMvc.perform(post("/mis-pedidos/" + order.getId() + "/cancelar").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mis-pedidos"))
                .andExpect(flash().attribute("successMsg", OrderService.MSG_CANCELADO_OK));

        Order cancelled = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELADO);
    }

    @Test
    @WithMockUser(username = "cliente.pedidos@himalaya.com", roles = {"CLIENT"})
    @DisplayName("US13 (CA13.2): deniega cancelación pasados 10 minutos con el mensaje exacto")
    void clientCannotCancelOrderAfterTenMinutes() throws Exception {
        Order oldOrder = crearPedido(OrderStatus.EN_PREPARACION, LocalDateTime.now().minusMinutes(15), 1);

        mockMvc.perform(post("/mis-pedidos/" + oldOrder.getId() + "/cancelar").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mis-pedidos"))
                .andExpect(flash().attribute("errorMsg", OrderService.MSG_CANCELAR_EXPIRADO));

        Order stillActive = orderRepository.findById(oldOrder.getId()).orElseThrow();
        assertThat(stillActive.getStatus()).isEqualTo(OrderStatus.EN_PREPARACION);
    }
}
