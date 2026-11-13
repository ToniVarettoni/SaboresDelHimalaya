package com.himalaya.sabor.controller;

import com.himalaya.sabor.dto.ProductDto;
import com.himalaya.sabor.model.Category;
import com.himalaya.sabor.model.Product;
import com.himalaya.sabor.model.ProductStatus;
import com.himalaya.sabor.service.CategoryService;
import com.himalaya.sabor.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/carta")
public class AdminCartaController {

    private final ProductService productService;
    private final CategoryService categoryService;

    public AdminCartaController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    // P6 - Panel de Administración: Gestión de la Carta y Bebidas (US5)
    @GetMapping
    public String showAdminCarta(Model model) {
        List<Product> products = productService.getAllProductsForAdmin();
        List<Category> categories = categoryService.findAll();

        model.addAttribute("products", products);
        model.addAttribute("categories", categories);
        if (!model.containsAttribute("productDto")) {
            model.addAttribute("productDto", new ProductDto());
        }
        model.addAttribute("statuses", ProductStatus.values());

        return "admin/carta";
    }

    // Alta de Producto (US5)
    @PostMapping
    public String createProduct(@Valid @ModelAttribute("productDto") ProductDto productDto,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("products", productService.getAllProductsForAdmin());
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("statuses", ProductStatus.values());
            model.addAttribute("openModal", true);
            return "admin/carta";
        }

        productService.saveProduct(productDto);
        redirectAttributes.addFlashAttribute("successMessage", "Producto registrado con éxito en la carta");
        return "redirect:/admin/carta";
    }

    // Formulario de edición
    @GetMapping("/editar/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        Product product = productService.getProductById(id);
        ProductDto dto = new ProductDto(
                product.getId(),
                product.getName(),
                product.getCategory().getId(),
                product.getDescription(),
                product.getPrice(),
                product.getStatus()
        );

        model.addAttribute("productDto", dto);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("statuses", ProductStatus.values());
        model.addAttribute("isEdit", true);

        return "admin/producto-form";
    }

    // Actualización de producto
    @PostMapping("/editar/{id}")
    public String updateProduct(@PathVariable("id") Long id,
                                @Valid @ModelAttribute("productDto") ProductDto productDto,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("statuses", ProductStatus.values());
            model.addAttribute("isEdit", true);
            return "admin/producto-form";
        }

        productDto.setId(id);
        productService.saveProduct(productDto);
        redirectAttributes.addFlashAttribute("successMessage", "Producto '" + productDto.getName() + "' actualizado correctamente");
        return "redirect:/admin/carta";
    }

    // Baja lógica / Cambio de estado (US5)
    @PostMapping("/{id}/estado")
    public String changeProductStatus(@PathVariable("id") Long id,
                                      @RequestParam("status") ProductStatus status,
                                      RedirectAttributes redirectAttributes) {
        try {
            Product product = productService.updateStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage",
                    "El estado de '" + product.getName() + "' ha cambiado a " + status.name());
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/carta";
    }
}
