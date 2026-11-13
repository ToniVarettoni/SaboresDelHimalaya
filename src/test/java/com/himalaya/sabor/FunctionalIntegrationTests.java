package com.himalaya.sabor;

import com.himalaya.sabor.dto.DailyMenuFormDto;
import com.himalaya.sabor.dto.DailyMenuItemDto;
import com.himalaya.sabor.model.*;
import com.himalaya.sabor.repository.DailyMenuRepository;
import com.himalaya.sabor.repository.OrderRepository;
import com.himalaya.sabor.repository.ProductRepository;
import com.himalaya.sabor.repository.UserRepository;
import com.himalaya.sabor.service.DailyMenuService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FunctionalIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private DailyMenuService dailyMenuService;
    @Autowired private DailyMenuRepository dailyMenuRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;

    private List<DailyMenuItemDto> platos(CourseType tipo, int n) {
        List<DailyMenuItemDto> lista = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            DailyMenuItemDto d = new DailyMenuItemDto();
            d.setCourseType(tipo);
            d.setCustomName(tipo.name() + " " + i);
            d.setCustomDescription("Plato de prueba");
            lista.add(d);
        }
        return lista;
    }

    private DailyMenuFormDto formulario(LocalDate fecha, int entrantes, int principales, int postres) {
        DailyMenuFormDto dto = new DailyMenuFormDto();
        dto.setDate(fecha);
        dto.setEntrantes(platos(CourseType.ENTRANTE, entrantes));
        dto.setPrincipales(platos(CourseType.PRINCIPAL, principales));
        dto.setPostres(platos(CourseType.POSTRE, postres));
        return dto;
    }

    // ===== US6: el chef elabora y publica el menú diario =====

    @Test
    @DisplayName("US6 (CA6.1): el chef publica un menú de 3 entrantes, 3 principales y 2 postres a 15 €")
    void chefPublishesValidMenu() {
        LocalDate fecha = LocalDate.now().plusDays(5);
        dailyMenuService.saveMenu(formulario(fecha, 3, 3, 2));
        DailyMenu menu = dailyMenuRepository.findByDate(fecha).orElseThrow();
        assertThat(menu.isPublished()).isTrue();
        assertThat(menu.getItems()).hasSize(8);
        assertThat(menu.getPrice()).isEqualByComparingTo(new BigDecimal("15.00"));
    }

    @Test
    @DisplayName("US6 (CA6.1): un menú con un número de platos incorrecto no se guarda")
    void incompleteMenuIsRejected() {
        LocalDate fecha = LocalDate.now().plusDays(6);
        assertThatThrownBy(() -> dailyMenuService.saveMenu(formulario(fecha, 2, 3, 2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("3 entrantes");
        assertThat(dailyMenuRepository.findByDate(fecha)).isEmpty();
    }

    @Test
    @WithMockUser(username = "admin@himalaya.com", roles = {"ADMIN"})
    @DisplayName("US5 (D-13): no se puede dar de baja un plato que está en el menú del día")
    void cannotDeactivateProductInDailyMenu() throws Exception {
        Product plato = productRepository.findAll().get(0);
        DailyMenu menu = new DailyMenu(LocalDate.now(), true);
        menu.addItem(new DailyMenuItem(menu, plato, CourseType.ENTRANTE));
        dailyMenuRepository.save(menu);

        mockMvc.perform(post("/admin/carta/" + plato.getId() + "/estado").with(csrf()).param("status", "INACTIVO"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/carta"))
                .andExpect(flash().attributeExists("errorMessage"));

        assertThat(productRepository.findById(plato.getId()).orElseThrow().getStatus()).isEqualTo(ProductStatus.ACTIVO);
    }

    // ===== US8 / US9: carrito y resumen =====

    @Test
    @WithMockUser(username = "cliente@test.com", roles = {"CLIENT"})
    @DisplayName("US8 (CA8.2) y US9 (CA9.1): la cantidad elegida se refleja en el importe total del resumen")
    void quantityIsReflectedInSummaryTotal() throws Exception {
        Product plato = productRepository.findAll().get(0);
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/carrito/agregar").session(session).with(csrf())
                        .param("productId", plato.getId().toString()).param("quantity", "2"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/pedido/resumen").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("pedido/resumen"))
                .andExpect(model().attribute("total", comparesEqualTo(plato.getPrice().multiply(BigDecimal.valueOf(2)))));
    }

    @Test
    @WithMockUser(username = "cliente@test.com", roles = {"CLIENT"})
    @DisplayName("US9: con el carrito vacío el resumen vuelve al carrito")
    void emptyCartSummaryRedirects() throws Exception {
        mockMvc.perform(get("/pedido/resumen").session(new MockHttpSession()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/carrito"));
    }

    // ===== US10: confirmación del pedido =====

    private User clienteDePrueba() {
        return userRepository.findByEmail("cliente.pedido@himalaya.com").orElseGet(() -> userRepository.save(
                new User("Cliente Pedido", "cliente.pedido@himalaya.com", "x", "Calle Mayor 1", "600000000", Role.CLIENT, UserStatus.ACTIVA)));
    }

    @Test
    @WithMockUser(username = "cliente.pedido@himalaya.com", roles = {"CLIENT"})
    @DisplayName("US10 (CA10.2): confirmar el pedido lo registra con número y estado RECIBIDO")
    void confirmOrderCreatesReceivedOrder() throws Exception {
        User cliente = clienteDePrueba();
        Product plato = productRepository.findAll().get(0);
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/carrito/agregar").session(session).with(csrf()).param("productId", plato.getId().toString()))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/pedido/confirmar").session(session).with(csrf())
                        .param("deliveryAddress", "Calle Mayor 1")
                        .param("phone", "600000000")
                        .param("paymentMethod", "EFECTIVO"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pedido/exito"))
                .andExpect(flash().attributeExists("orderNumber"));

        Order pedido = orderRepository.findAll().stream()
                .filter(o -> o.getUser().getId().equals(cliente.getId())).findFirst().orElseThrow();
        assertThat(pedido.getStatus()).isEqualTo(OrderStatus.RECIBIDO);
        assertThat(pedido.getOrderNumber()).startsWith("PED-");
        assertThat(pedido.getTotalAmount()).isEqualByComparingTo(plato.getPrice());
    }

    @Test
    @WithMockUser(username = "cliente.pedido@himalaya.com", roles = {"CLIENT"})
    @DisplayName("US10 (CA10.1): sin dirección de entrega el pedido no se confirma")
    void confirmOrderWithoutAddressIsRejected() throws Exception {
        clienteDePrueba();
        Product plato = productRepository.findAll().get(0);
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/carrito/agregar").session(session).with(csrf()).param("productId", plato.getId().toString()))
                .andExpect(status().is3xxRedirection());
        long antes = orderRepository.count();
        mockMvc.perform(post("/pedido/confirmar").session(session).with(csrf())
                        .param("deliveryAddress", "")
                        .param("phone", "600000000")
                        .param("paymentMethod", "EFECTIVO"))
                .andExpect(status().isOk())
                .andExpect(view().name("pedido/resumen"))
                .andExpect(model().attributeHasFieldErrors("orderDto", "deliveryAddress"));
        assertThat(orderRepository.count()).isEqualTo(antes);
    }
}
