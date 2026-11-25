package com.himalaya.sabor.controller;

import com.himalaya.sabor.dto.OrderModifyDto;
import com.himalaya.sabor.model.Order;
import com.himalaya.sabor.model.OrderStatus;
import com.himalaya.sabor.service.OrderService;
import com.himalaya.sabor.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

/**
 * Controlador de historial, detalle, modificación y cancelación de pedidos del cliente (US11, US12, US13).
 */
@Controller
@RequestMapping("/mis-pedidos")
public class ClientOrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductService productService;

    /** P10 – Histórico y seguimiento completo de pedidos del cliente (US11) */
    @GetMapping({"", "/"})
    public String listOrders(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        List<Order> orders = orderService.getOrdersByUserEmail(principal.getName());
        model.addAttribute("orders", orders);
        return "mis-pedidos/index";
    }

    /** CA11.2 – Detalle completo de un pedido seleccionado */
    @GetMapping("/{id}")
    public String orderDetail(@PathVariable Long id,
                              Model model,
                              Principal principal,
                              RedirectAttributes redirectAttrs) {
        if (principal == null) {
            return "redirect:/login";
        }
        try {
            Order order = orderService.getOrderForUser(id, principal.getName());
            model.addAttribute("order", order);
            return "mis-pedidos/detalle";
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/mis-pedidos";
        }
    }

    /** P11 – Formulario de modificación de pedido dentro de la ventana de 10 minutos (US12) */
    @GetMapping("/{id}/modificar")
    public String showModifyForm(@PathVariable Long id,
                                 Model model,
                                 Principal principal,
                                 RedirectAttributes redirectAttrs) {
        if (principal == null) {
            return "redirect:/login";
        }
        try {
            Order order = orderService.getOrderForUser(id, principal.getName());
            if (!order.isWithinTenMinutes()) {
                redirectAttrs.addFlashAttribute("errorMsg", OrderService.MSG_MODIFICAR_EXPIRADO);
                return "redirect:/mis-pedidos/" + id;
            }
            if (order.getStatus() == OrderStatus.CANCELADO || order.getStatus() == OrderStatus.ENTREGADO) {
                redirectAttrs.addFlashAttribute("errorMsg",
                        "No es posible modificar un pedido en estado " + order.getStatus().getDisplayName());
                return "redirect:/mis-pedidos/" + id;
            }

            model.addAttribute("order", order);
            model.addAttribute("modifyDto", orderService.toModifyDto(order));
            model.addAttribute("activeProducts", productService.findAllActive());
            return "mis-pedidos/modificar";
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/mis-pedidos";
        }
    }

    /** POST – Guarda la modificación del pedido (US12) */
    @PostMapping("/{id}/modificar")
    public String processModify(@PathVariable Long id,
                                @Valid @ModelAttribute("modifyDto") OrderModifyDto dto,
                                BindingResult result,
                                Principal principal,
                                Model model,
                                RedirectAttributes redirectAttrs) {
        if (principal == null) {
            return "redirect:/login";
        }

        Order order;
        try {
            order = orderService.getOrderForUser(id, principal.getName());
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/mis-pedidos";
        }

        if (!order.isWithinTenMinutes()) {
            redirectAttrs.addFlashAttribute("errorMsg", OrderService.MSG_MODIFICAR_EXPIRADO);
            return "redirect:/mis-pedidos/" + id;
        }

        if (result.hasErrors()) {
            model.addAttribute("order", order);
            model.addAttribute("activeProducts", productService.findAllActive());
            return "mis-pedidos/modificar";
        }

        try {
            orderService.updateOrder(id, principal.getName(), dto);
            redirectAttrs.addFlashAttribute("successMsg", "Pedido actualizado correctamente.");
            return "redirect:/mis-pedidos/" + id;
        } catch (IllegalStateException e) {
            redirectAttrs.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/mis-pedidos/" + id;
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMsg", e.getMessage());
            model.addAttribute("order", order);
            model.addAttribute("activeProducts", productService.findAllActive());
            return "mis-pedidos/modificar";
        }
    }

    /** POST – Cancelación del pedido dentro de la ventana de 10 minutos (US13) */
    @PostMapping("/{id}/cancelar")
    public String cancelOrder(@PathVariable Long id,
                              Principal principal,
                              RedirectAttributes redirectAttrs) {
        if (principal == null) {
            return "redirect:/login";
        }
        try {
            orderService.cancelOrder(id, principal.getName());
            redirectAttrs.addFlashAttribute("successMsg", OrderService.MSG_CANCELADO_OK);
            return "redirect:/mis-pedidos";
        } catch (IllegalStateException | IllegalArgumentException | SecurityException e) {
            redirectAttrs.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/mis-pedidos";
        }
    }
}
