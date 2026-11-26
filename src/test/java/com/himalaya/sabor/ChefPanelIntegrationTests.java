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
class ChefPanelIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    @WithMockUser(username = "chef@himalaya.com", roles = {"CHEF"})
    @DisplayName("Chef Menú Diario: filtra platos por tipo de curso en cada sección")
    void chefMenuFiltersProductsByCourseType() throws Exception {
        mockMvc.perform(get("/chef/menu-diario"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("entrantesProducts", "principalesProducts", "postresProducts"))
                .andExpect(content().string(containsString("Samosa Nepalí")))
                .andExpect(content().string(containsString("Pollo Tandoori")))
                .andExpect(content().string(containsString("Kheer Nepalí")));
    }

    @Test
    @WithMockUser(username = "chef@himalaya.com", roles = {"CHEF"})
    @DisplayName("Chef Pedidos: muestra pedidos de hoy, agrupa anteriores y avanza Recibido → En preparación → En reparto con mensaje personalizado")
    void chefPedidosByDateAndUpdateStatusToEnReparto() throws Exception {
        User cliente = userRepository.save(new User("María López", "maria.chef.test@himalaya.com",
                passwordEncoder.encode("pass123"), "Calle Luna 5", "611222333", Role.CLIENT, UserStatus.ACTIVA));
        Product p = productRepository.findAll().get(0);

        Order hoy = new Order();
        hoy.setOrderNumber("PED-HOY-001");
        hoy.setUser(cliente);
        hoy.setDeliveryAddress("Calle Luna 5");
        hoy.setPhone("611222333");
        hoy.setPaymentMethod(PaymentMethod.EFECTIVO);
        hoy.setStatus(OrderStatus.RECIBIDO);
        hoy.setCreatedAt(LocalDateTime.now());
        hoy.setTotalAmount(p.getPrice());
        hoy.addItem(new OrderItem(hoy, p, p.getPrice(), 1, p.getPrice()));
        orderRepository.save(hoy);

        Order ayer = new Order();
        ayer.setOrderNumber("PED-AYER-001");
        ayer.setUser(cliente);
        ayer.setDeliveryAddress("Calle Luna 5");
        ayer.setPhone("611222333");
        ayer.setPaymentMethod(PaymentMethod.TARJETA);
        ayer.setStatus(OrderStatus.ENTREGADO);
        ayer.setCreatedAt(LocalDateTime.now().minusDays(1));
        ayer.setTotalAmount(p.getPrice());
        ayer.addItem(new OrderItem(ayer, p, p.getPrice(), 1, p.getPrice()));
        orderRepository.save(ayer);

        mockMvc.perform(get("/chef/pedidos"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("PED-HOY-001")))
                .andExpect(content().string(containsString("Mostrar pedidos anteriores")))
                .andExpect(content().string(containsString("PED-AYER-001")));

        mockMvc.perform(post("/chef/pedidos/" + hoy.getId() + "/estado")
                        .with(csrf())
                        .param("status", "EN_PREPARACION"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/chef/pedidos/" + hoy.getId() + "/estado")
                        .with(csrf())
                        .param("status", "EN_REPARTO"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/chef/pedidos"))
                .andExpect(flash().attribute("successMsg", "Pedido para María López cambió a estado En reparto"));

        Order updated = orderRepository.findById(hoy.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.EN_REPARTO);
    }

    @Test
    @WithMockUser(username = "chef@himalaya.com", roles = {"CHEF"})
    @DisplayName("US14 (CA14.1): el estado avanza de forma secuencial; no se puede saltar de Recibido a Entregado")
    void chefCannotSkipOrderStates() throws Exception {
        User cliente = userRepository.save(new User("Pablo Ruiz", "pablo.chef.test@himalaya.com",
                passwordEncoder.encode("pass123"), "Calle Sol 3", "622333444", Role.CLIENT, UserStatus.ACTIVA));
        Product p = productRepository.findAll().get(0);
        Order pedido = new Order();
        pedido.setOrderNumber("PED-SALTO-001");
        pedido.setUser(cliente);
        pedido.setDeliveryAddress("Calle Sol 3");
        pedido.setPhone("622333444");
        pedido.setPaymentMethod(PaymentMethod.EFECTIVO);
        pedido.setStatus(OrderStatus.RECIBIDO);
        pedido.setCreatedAt(LocalDateTime.now());
        pedido.setTotalAmount(p.getPrice());
        pedido.addItem(new OrderItem(pedido, p, p.getPrice(), 1, p.getPrice()));
        orderRepository.save(pedido);

        mockMvc.perform(post("/chef/pedidos/" + pedido.getId() + "/estado")
                        .with(csrf())
                        .param("status", "ENTREGADO"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/chef/pedidos"))
                .andExpect(flash().attributeExists("errorMsg"));

        assertThat(orderRepository.findById(pedido.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.RECIBIDO);
    }
}
