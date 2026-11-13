package com.himalaya.sabor;

import com.himalaya.sabor.model.*;
import com.himalaya.sabor.repository.DailyMenuRepository;
import com.himalaya.sabor.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CartMenuIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DailyMenuRepository dailyMenuRepository;

    @Autowired
    private ProductRepository productRepository;

    /** Crea y publica un menú para hoy -----> 1 entrante, 1 principal y 1 postre. */
    private DailyMenu crearMenuDeHoy() {
        List<Product> productos = productRepository.findAll();
        DailyMenu menu = new DailyMenu(LocalDate.now(), true);
        menu.addItem(new DailyMenuItem(menu, productos.get(0), CourseType.ENTRANTE));
        menu.addItem(new DailyMenuItem(menu, productos.get(1), CourseType.PRINCIPAL));
        menu.addItem(new DailyMenuItem(menu, "Postre de la casa", "Postre casero del día", CourseType.POSTRE));
        return dailyMenuRepository.save(menu);
    }

    private Long idDe(DailyMenu menu, CourseType tipo) {
        return menu.getItems().stream().filter(i -> i.getCourseType() == tipo).findFirst().orElseThrow().getId();
    }

    @Test
    @DisplayName("US8 (D-12): un usuario sin sesión no puede añadir al carrito y va al inicio de sesión")
    void anonymousCannotAddToCart() throws Exception {
        Long productId = productRepository.findAll().get(0).getId();
        mockMvc.perform(post("/carrito/agregar").with(csrf()).param("productId", productId.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "cliente@test.com", roles = {"CLIENT"})
    @DisplayName("US8 (CA8.1): sin menú publicado hoy se informa de que no está disponible")
    void menuNotAvailableMessage() throws Exception {
        dailyMenuRepository.findByDate(LocalDate.now()).ifPresent(dailyMenuRepository::delete);
        mockMvc.perform(get("/carrito/configurar-menu"))
                .andExpect(status().isOk())
                .andExpect(view().name("carrito/configurar-menu"))
                .andExpect(content().string(containsString("El menú del día no está disponible hoy.")));
    }

    @Test
    @WithMockUser(username = "cliente@test.com", roles = {"CLIENT"})
    @DisplayName("US8 (CA8.1): elegir entrante, principal y postre añade una sola línea de 15 € al carrito")
    void addDailyMenuAsSingleLine() throws Exception {
        DailyMenu menu = crearMenuDeHoy();
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/carrito/configurar-menu").session(session).with(csrf())
                        .param("entranteId", idDe(menu, CourseType.ENTRANTE).toString())
                        .param("principalId", idDe(menu, CourseType.PRINCIPAL).toString())
                        .param("postreId", idDe(menu, CourseType.POSTRE).toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/carrito"));
        mockMvc.perform(get("/carrito").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Menú del día")))
                .andExpect(content().string(containsString("Postre de la casa")));
    }

    @Test
    @WithMockUser(username = "cliente@test.com", roles = {"CLIENT"})
    @DisplayName("US8 (CA8.1): si falta un plato del menú no se añade y se muestra el aviso")
    void incompleteMenuIsRejected() throws Exception {
        DailyMenu menu = crearMenuDeHoy();
        mockMvc.perform(post("/carrito/configurar-menu").with(csrf())
                        .param("entranteId", idDe(menu, CourseType.ENTRANTE).toString())
                        .param("principalId", idDe(menu, CourseType.PRINCIPAL).toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/carrito/configurar-menu"))
                .andExpect(flash().attribute("menuError", "Elige un entrante, un plato principal y un postre."));
    }

    @Test
    @DisplayName("US8: dos sesiones distintas tienen carritos distintos")
    @WithMockUser(username = "cliente@test.com", roles = {"CLIENT"})
    void cartIsPerSession() throws Exception {
        Long productId = productRepository.findAll().get(0).getId();
        String nombre = productRepository.findAll().get(0).getName();
        MockHttpSession sesionA = new MockHttpSession();
        MockHttpSession sesionB = new MockHttpSession();
        mockMvc.perform(post("/carrito/agregar").session(sesionA).with(csrf()).param("productId", productId.toString()))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/carrito").session(sesionB))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Tu carrito está vacío")));
        mockMvc.perform(get("/carrito").session(sesionA))
                .andExpect(content().string(containsString(nombre)));
    }
}
