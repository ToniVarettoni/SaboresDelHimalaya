package com.himalaya.sabor;

import com.himalaya.sabor.model.*;
import com.himalaya.sabor.repository.OrderRepository;
import com.himalaya.sabor.repository.ProductRepository;
import com.himalaya.sabor.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Pruebas de sistema: un cliente solo puede ver y cancelar sus propios pedidos (US11 y US13). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ClientOrderSecurityIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    /** Pedido reciente de otro cliente (dueno.pedido@himalaya.com). */
    private Order pedidoDeOtroCliente() {
        User dueno = userRepository.findByEmail("dueno.pedido@himalaya.com").orElseGet(() ->
                userRepository.save(new User("Dueño Pedido", "dueno.pedido@himalaya.com",
                        passwordEncoder.encode("pass123"), "Calle Mar 2", "633444555", Role.CLIENT, UserStatus.ACTIVA)));
        Product p = productRepository.findAll().get(0);
        Order o = new Order();
        o.setOrderNumber("PED-AJENO-" + System.nanoTime());
        o.setUser(dueno);
        o.setDeliveryAddress("Calle Mar 2");
        o.setPhone("633444555");
        o.setPaymentMethod(PaymentMethod.EFECTIVO);
        o.setStatus(OrderStatus.RECIBIDO);
        o.setCreatedAt(LocalDateTime.now().minusMinutes(2));
        o.setTotalAmount(p.getPrice());
        o.addItem(new OrderItem(o, p, p.getPrice(), 1, p.getPrice()));
        return orderRepository.save(o);
    }

    private void crearClienteIntruso() {
        userRepository.findByEmail("intruso@himalaya.com").orElseGet(() ->
                userRepository.save(new User("Intruso", "intruso@himalaya.com",
                        passwordEncoder.encode("pass123"), "Calle Luz 9", "644555666", Role.CLIENT, UserStatus.ACTIVA)));
    }

    @Test
    @WithMockUser(username = "intruso@himalaya.com", roles = {"CLIENT"})
    @DisplayName("US11 (caso negativo): un cliente no puede ver el detalle del pedido de otro cliente")
    void clientCannotSeeAnotherClientsOrder() throws Exception {
        crearClienteIntruso();
        Order ajeno = pedidoDeOtroCliente();
        mockMvc.perform(get("/mis-pedidos/" + ajeno.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mis-pedidos"))
                .andExpect(flash().attributeExists("errorMsg"));
    }

    @Test
    @WithMockUser(username = "intruso@himalaya.com", roles = {"CLIENT"})
    @DisplayName("US13 (caso negativo): un cliente no puede cancelar el pedido de otro cliente")
    void clientCannotCancelAnotherClientsOrder() throws Exception {
        crearClienteIntruso();
        Order ajeno = pedidoDeOtroCliente();
        mockMvc.perform(post("/mis-pedidos/" + ajeno.getId() + "/cancelar").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mis-pedidos"))
                .andExpect(flash().attributeExists("errorMsg"));
        assertThat(orderRepository.findById(ajeno.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.RECIBIDO);
    }
}
