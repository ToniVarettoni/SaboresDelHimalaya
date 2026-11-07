package com.himalaya.sabor.controller;

import com.himalaya.sabor.service.CartService;
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
