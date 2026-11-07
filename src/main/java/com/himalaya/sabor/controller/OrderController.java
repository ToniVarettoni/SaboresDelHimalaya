package com.himalaya.sabor.controller;

import com.himalaya.sabor.dto.OrderConfirmDto;
import com.himalaya.sabor.model.Order;
import com.himalaya.sabor.model.PaymentMethod;
import com.himalaya.sabor.service.CartService;
import com.himalaya.sabor.service.OrderService;
import com.himalaya.sabor.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * Gestiona el flujo de pedido:
 *   GET  /pedido/resumen   → P8: resumen + formulario de envío/pago
 *   POST /pedido/confirmar → persiste el pedido y redirige a P9 (éxito)
 */
@Controller
@RequestMapping("/pedido")
public class OrderController {

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    /** P8 – Resumen de Pedido, Envío y Pago */
    @GetMapping("/resumen")
    public String resumen(Model model, Principal principal) {
        if (cartService.getItems().isEmpty()) {
            return "redirect:/carrito";
        }
        model.addAttribute("items", cartService.getItems());
        model.addAttribute("total", cartService.getTotal());

        // Pre-rellenar dirección y teléfono del perfil del usuario si están disponibles
        OrderConfirmDto dto = new OrderConfirmDto();
        if (principal != null) {
            userRepository.findByEmail(principal.getName()).ifPresent(user -> {
                if (user.getAddress() != null) dto.setDeliveryAddress(user.getAddress());
                if (user.getPhone() != null) dto.setPhone(user.getPhone());
            });
        }

        model.addAttribute("orderDto", dto);
        model.addAttribute("paymentMethods", PaymentMethod.values());
        return "pedido/resumen";
    }

    /** POST – Confirma y persiste el pedido (P8 → P9) */
    @PostMapping("/confirmar")
    public String confirmar(@Valid @ModelAttribute("orderDto") OrderConfirmDto dto,
                            BindingResult result,
                            Principal principal,
                            Model model,
                            RedirectAttributes redirectAttrs) {

        if (result.hasErrors()) {
            model.addAttribute("items", cartService.getItems());
            model.addAttribute("total", cartService.getTotal());
            model.addAttribute("paymentMethods", PaymentMethod.values());
            return "pedido/resumen";
        }

        if (principal == null) {
            return "redirect:/login";
        }

        try {
            Order order = orderService.placeOrder(cartService, dto, principal.getName());
            redirectAttrs.addFlashAttribute("orderNumber", order.getOrderNumber());
            redirectAttrs.addFlashAttribute("orderCreatedAt", order.getCreatedAt());
            redirectAttrs.addFlashAttribute("orderTotal", order.getTotalAmount());
            return "redirect:/pedido/exito";
        } catch (Exception e) {
            model.addAttribute("errorMsg", "Error al procesar el pedido: " + e.getMessage());
            model.addAttribute("items", cartService.getItems());
            model.addAttribute("total", cartService.getTotal());
            model.addAttribute("paymentMethods", PaymentMethod.values());
            return "pedido/resumen";
        }
    }

    /** P9 – Pantalla de éxito */
    @GetMapping("/exito")
    public String exito(Model model) {
        if (!model.containsAttribute("orderNumber")) {
            return "redirect:/carta";
        }
        return "pedido/exito";
    }
}
