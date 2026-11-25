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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminBillingIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Order crearPedido(User cliente, Product p, OrderStatus status, LocalDateTime fecha, String precio) {
        Order o = new Order();
        o.setOrderNumber("PED-BILL-" + System.nanoTime());
        o.setUser(cliente);
        o.setDeliveryAddress("Calle Mayor 1");
        o.setPhone("600111222");
        o.setPaymentMethod(PaymentMethod.TARJETA);
        o.setStatus(status);
        o.setCreatedAt(fecha);
        BigDecimal total = new BigDecimal(precio);
        o.setTotalAmount(total);
        o.addItem(new OrderItem(o, p, total, 1, total));
        return orderRepository.save(o);
    }

    @Test
    @WithMockUser(username = "cliente@himalaya.com", roles = {"CLIENT"})
    @DisplayName("US15: un cliente no puede acceder al informe de facturación (solo ADMIN)")
    void clientCannotAccessBillingReport() throws Exception {
        mockMvc.perform(get("/admin/facturacion"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "chef@himalaya.com", roles = {"CHEF"})
    @DisplayName("US15: un chef no puede acceder al informe de facturación (solo ADMIN)")
    void chefCannotAccessBillingReport() throws Exception {
        mockMvc.perform(get("/admin/facturacion"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@himalaya.com", roles = {"ADMIN"})
    @DisplayName("US15 (CA15.1, CA15.2): calcula pedidos completados y total facturado de una fecha")
    void adminViewsCompletedOrdersAndTotalsForDate() throws Exception {
        User cliente = userRepository.save(new User("Carlos Ruiz", "carlos.bill@himalaya.com",
                passwordEncoder.encode("pass123"), "Calle Mayor 1", "600111222", Role.CLIENT, UserStatus.ACTIVA));
        Product p = productRepository.findAll().get(0);

        LocalDate fecha = LocalDate.now().minusDays(2);
        Order completado1 = crearPedido(cliente, p, OrderStatus.ENTREGADO, fecha.atTime(13, 0), "15.00");
        Order completado2 = crearPedido(cliente, p, OrderStatus.ENTREGADO, fecha.atTime(20, 30), "22.50");
        // No completado: no debe sumarse
        Order cancelado = crearPedido(cliente, p, OrderStatus.CANCELADO, fecha.atTime(14, 0), "99.00");

        mockMvc.perform(get("/admin/facturacion")
                        .param("fecha", fecha.toString())
                        .param("dias", "7"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("totalOrdersCount", 2))
                .andExpect(model().attribute("totalBilled", comparesEqualTo(new BigDecimal("37.50"))))
                .andExpect(model().attribute("chartLabels", hasSize(7)))
                .andExpect(content().string(containsString(completado1.getOrderNumber())))
                .andExpect(content().string(containsString(completado2.getOrderNumber())))
                .andExpect(content().string(containsString("Carlos Ruiz")))
                .andExpect(content().string(not(containsString(cancelado.getOrderNumber()))));
    }

    @Test
    @WithMockUser(username = "admin@himalaya.com", roles = {"ADMIN"})
    @DisplayName("US15 (CA15.3): muestra 0 € y el mensaje literal cuando no hay ventas en la fecha")
    void adminViewsEmptyMessageWhenNoSalesOnDate() throws Exception {
        LocalDate fechaSinVentas = LocalDate.now().minusDays(10);

        mockMvc.perform(get("/admin/facturacion").param("fecha", fechaSinVentas.toString()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("totalOrdersCount", 0))
                .andExpect(content().string(containsString("0 €")))
                .andExpect(content().string(containsString("No hay facturación registrada para la fecha seleccionada")));
    }
}
