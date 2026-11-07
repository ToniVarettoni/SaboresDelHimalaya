package com.himalaya.sabor;

import com.himalaya.sabor.model.Category;
import com.himalaya.sabor.model.Product;
import com.himalaya.sabor.model.ProductStatus;
import com.himalaya.sabor.repository.CategoryRepository;
import com.himalaya.sabor.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CartaIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    // ==========================================
    // US4 - CONSULTA DE LA CARTA (PÚBLICA / CLIENTES)
    // ==========================================

    @Test
    @DisplayName("US4: La carta pública (/carta) es accesible sin login y lista platos agrupados por categorías")
    void publicCartaIsAccessibleAndShowsCategories() throws Exception {
        mockMvc.perform(get("/carta"))
                .andExpect(status().isOk())
                .andExpect(view().name("carta/index"))
                .andExpect(content().string(containsString("Nuestra Carta")))
                .andExpect(content().string(containsString("Entrantes")))
                .andExpect(content().string(containsString("Tandoori")))
                .andExpect(content().string(containsString("Samosa Nepalí")))
                .andExpect(content().string(containsString("Pollo Tandoori")));
    }

    @Test
    @DisplayName("US4: El filtrado por categoría muestra únicamente los platos de la categoría seleccionada")
    void filterCartaByCategory() throws Exception {
        mockMvc.perform(get("/carta").param("categoria", "entrantes"))
                .andExpect(status().isOk())
                .andExpect(view().name("carta/index"))
                .andExpect(content().string(containsString("Samosa Nepalí")))
                .andExpect(content().string(containsString("Tarkari Raas")));
    }

    @Test
    @DisplayName("US4: Los productos con estado INACTIVO o DESCATALOGADO se ocultan de la vista de clientes")
    void inactiveProductsAreHiddenFromClients() throws Exception {
        Category entrantes = categoryRepository.findBySlug("entrantes").orElseThrow();
        Product hiddenDish = new Product(
                entrantes,
                "Plato Secreto Oculto",
                "Ingredientes secretos que no deben verse",
                new BigDecimal("9.99"),
                ProductStatus.INACTIVO
        );
        productRepository.save(hiddenDish);

        mockMvc.perform(get("/carta"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Plato Secreto Oculto"))));
    }

    // ==========================================
    // US5 - GESTIÓN DE PRODUCTOS DE LA CARTA (ADMINISTRADOR)
    // ==========================================

    @Test
    @DisplayName("US5: Acceso a /admin/carta denegado a usuarios anónimos (redirige a login)")
    void adminCartaDeniedToAnonymous() throws Exception {
        mockMvc.perform(get("/admin/carta"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "cliente@himalaya.com", roles = {"CLIENT"})
    @DisplayName("US5: Acceso a /admin/carta denegado con rol CLIENT (403 Forbidden)")
    void adminCartaDeniedToClient() throws Exception {
        mockMvc.perform(get("/admin/carta"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@himalaya.com", roles = {"ADMIN"})
    @DisplayName("US5: Acceso a /admin/carta permitido a administradores con lista de catálogo")
    void adminCartaAllowedToAdmin() throws Exception {
        mockMvc.perform(get("/admin/carta"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/carta"))
                .andExpect(content().string(containsString("Gestión de la Carta y Bebidas")))
                .andExpect(content().string(containsString("Añadir Nuevo Producto")));
    }

    @Test
    @WithMockUser(username = "admin@himalaya.com", roles = {"ADMIN"})
    @DisplayName("US5: Alta de producto en la carta por el administrador")
    void createProductSuccessfully() throws Exception {
        Category tandoori = categoryRepository.findBySlug("tandoori").orElseThrow();

        mockMvc.perform(post("/admin/carta")
                        .with(csrf())
                        .param("name", "Cordero Tikka Himalaya")
                        .param("categoryId", tandoori.getId().toString())
                        .param("description", "Trozos de pierna de cordero marinados con hierbas del Himalaya")
                        .param("price", "14.50")
                        .param("status", "ACTIVO"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/carta"))
                .andExpect(flash().attributeExists("successMessage"));

        boolean exists = productRepository.findAll().stream()
                .anyMatch(p -> p.getName().equals("Cordero Tikka Himalaya") && p.getPrice().compareTo(new BigDecimal("14.50")) == 0);
        assertThat(exists).isTrue();
    }

    @Test
    @WithMockUser(username = "admin@himalaya.com", roles = {"ADMIN"})
    @DisplayName("US5: Edición y actualización de un producto existente")
    void updateProductSuccessfully() throws Exception {
        Product sample = productRepository.findAll().get(0);
        Category category = sample.getCategory();

        mockMvc.perform(post("/admin/carta/editar/" + sample.getId())
                        .with(csrf())
                        .param("id", sample.getId().toString())
                        .param("name", sample.getName() + " - Edición Gourmet")
                        .param("categoryId", category.getId().toString())
                        .param("description", "Nueva descripción actualizada con azafrán extra")
                        .param("price", "8.90")
                        .param("status", "ACTIVO"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/carta"))
                .andExpect(flash().attributeExists("successMessage"));

        Product updated = productRepository.findById(sample.getId()).orElseThrow();
        assertThat(updated.getName()).endsWith("- Edición Gourmet");
        assertThat(updated.getPrice()).isEqualByComparingTo(new BigDecimal("8.90"));
    }

    @Test
    @WithMockUser(username = "admin@himalaya.com", roles = {"ADMIN"})
    @DisplayName("US5: Baja lógica (cambiar estado a INACTIVO) oculta el producto de clientes")
    void logicalDeletionHidesProductFromCarta() throws Exception {
        Product sample = productRepository.findAll().get(0);
        Long sampleId = sample.getId();
        String sampleName = sample.getName();

        // Admin cambia estado a INACTIVO
        mockMvc.perform(post("/admin/carta/" + sampleId + "/estado")
                        .with(csrf())
                        .param("status", "INACTIVO"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/carta"));

        Product afterUpdate = productRepository.findById(sampleId).orElseThrow();
        assertThat(afterUpdate.getStatus()).isEqualTo(ProductStatus.INACTIVO);

        // Cliente consulta la carta y no ve el plato
        mockMvc.perform(get("/carta"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(sampleName))));
    }
}
