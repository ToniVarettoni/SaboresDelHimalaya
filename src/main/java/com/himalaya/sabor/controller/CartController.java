package com.himalaya.sabor.controller;

import com.himalaya.sabor.model.CourseType;
import com.himalaya.sabor.model.DailyMenu;
import com.himalaya.sabor.service.CartService;
import com.himalaya.sabor.service.DailyMenuService;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador del carrito de compra. Las operaciones son a nivel de sesión y
 * no persisten en la base de datos.
 */
@Controller
@RequestMapping("/carrito")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private DailyMenuService dailyMenuService;

    /** Pantalla para elegir los platos del menú del día (US8, CA8.1). */
    @GetMapping("/configurar-menu")
    public String configurarMenu(Model model) {
        Optional<DailyMenu> menu = dailyMenuService.findTodayMenu();
        model.addAttribute("dailyMenu", menu.orElse(null));
        menu.ifPresent(m -> {
            model.addAttribute("entrantes", m.getItems().stream().filter(i -> i.getCourseType() == CourseType.ENTRANTE).toList());
            model.addAttribute("principales", m.getItems().stream().filter(i -> i.getCourseType() == CourseType.PRINCIPAL).toList());
            model.addAttribute("postres", m.getItems().stream().filter(i -> i.getCourseType() == CourseType.POSTRE).toList());
        });
        return "carrito/configurar-menu";
    }

    @PostMapping("/configurar-menu")
    public String anadirMenu(@RequestParam(required = false) Long entranteId,
                             @RequestParam(required = false) Long principalId,
                             @RequestParam(required = false) Long postreId,
                             @RequestParam(defaultValue = "1") int quantity,
                             RedirectAttributes redirectAttributes) {
        try {
            cartService.addDailyMenu(entranteId, principalId, postreId, quantity);
            return "redirect:/carrito";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("menuError", e.getMessage());
            return "redirect:/carrito/configurar-menu";
        }
    }

    @GetMapping
    public String viewCart(Model model) {
        model.addAttribute("items", cartService.getItems());
        model.addAttribute("total", cartService.getTotal());
        return "carrito/index";
    }

    /** Usado por los formularios de la carta (productId como parámetro de formulario). */
    @PostMapping("/agregar")
    public String addProductForm(@RequestParam Long productId,
                                 @RequestParam(defaultValue = "1") int quantity) {
        cartService.addProduct(productId, quantity);
        return "redirect:/carta?added=true";
    }

    /** Usado por enlaces directos con path variable. */
    @PostMapping("/add/{productId}")
    public String addProduct(@PathVariable Long productId,
                             @RequestParam(defaultValue = "1") int quantity) {
        cartService.addProduct(productId, quantity);
        return "redirect:/carta?added=true";
    }

    @PostMapping("/add-daily/{dailyMenuItemId}")
    public String addDailyMenuItem(@PathVariable Long dailyMenuItemId,
                                   @RequestParam(defaultValue = "1") int quantity) {
        cartService.addDailyMenuItem(dailyMenuItemId, quantity);
        return "redirect:/carrito";
    }

    @PostMapping("/update/{cartItemId}")
    public String updateQuantity(@PathVariable String cartItemId,
                                 @RequestParam int quantity) {
        cartService.updateQuantity(cartItemId, quantity);
        return "redirect:/carrito";
    }

    @PostMapping("/remove/{cartItemId}")
    public String removeItem(@PathVariable String cartItemId) {
        cartService.removeItem(cartItemId);
        return "redirect:/carrito";
    }

    @PostMapping("/clear")
    public String clearCart() {
        cartService.clear();
        return "redirect:/carrito";
    }
}
