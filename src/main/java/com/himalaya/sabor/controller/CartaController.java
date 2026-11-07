package com.himalaya.sabor.controller;

import com.himalaya.sabor.model.Category;
import com.himalaya.sabor.model.DailyMenu;
import com.himalaya.sabor.model.Product;
import com.himalaya.sabor.repository.DailyMenuRepository;
import com.himalaya.sabor.service.CategoryService;
import com.himalaya.sabor.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class CartaController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final DailyMenuRepository dailyMenuRepository;

    public CartaController(ProductService productService,
                           CategoryService categoryService,
                           DailyMenuRepository dailyMenuRepository) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.dailyMenuRepository = dailyMenuRepository;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/carta";
    }

    // P1 - Página Principal y Catálogo (Home) - US4
    @GetMapping("/carta")
    public String showCarta(@RequestParam(name = "categoria", required = false) String categorySlug, Model model) {
        List<Category> allCategories = categoryService.findAll();
        model.addAttribute("categories", allCategories);
        model.addAttribute("activeSlug", categorySlug);

        // Menú Diario para hoy
        Optional<DailyMenu> todayDailyMenu = dailyMenuRepository.findByDateAndIsPublishedTrue(LocalDate.now());
        model.addAttribute("dailyMenu", todayDailyMenu.orElse(null));

        if (categorySlug != null && !categorySlug.isBlank()) {
            Optional<Category> selectedCategory = categoryService.findBySlug(categorySlug);
            if (selectedCategory.isPresent()) {
                List<Product> products = productService.getActiveProductsByCategorySlug(categorySlug);
                model.addAttribute("selectedCategory", selectedCategory.get());
                model.addAttribute("categoryProducts", products);
                model.addAttribute("isFiltered", true);
            } else {
                model.addAttribute("isFiltered", false);
                model.addAttribute("groupedProducts", productService.getActiveProductsGroupedByCategory());
            }
        } else {
            model.addAttribute("isFiltered", false);
            model.addAttribute("groupedProducts", productService.getActiveProductsGroupedByCategory());
        }

        return "carta/index";
    }
}
