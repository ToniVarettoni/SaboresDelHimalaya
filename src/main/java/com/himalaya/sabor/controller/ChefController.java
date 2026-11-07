package com.himalaya.sabor.controller;

import com.himalaya.sabor.dto.DailyMenuFormDto;
import com.himalaya.sabor.model.Order;
import com.himalaya.sabor.model.OrderStatus;
import com.himalaya.sabor.repository.OrderRepository;
import com.himalaya.sabor.service.DailyMenuService;
import com.himalaya.sabor.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

/**
 * Panel del Chef – protegido por ROLE_CHEF (SecurityConfig).
 *
 * GET  /chef             → panel principal (dashboard)
 * GET  /chef/menu-diario → formulario menú diario (nuevo o edición)
 * POST /chef/menu-diario → guarda/publica el menú
 * GET  /chef/pedidos     → lista de todos los pedidos
 * POST /chef/pedidos/{id}/estado → actualiza el estado de un pedido
 */
@Controller
@RequestMapping("/chef")
public class ChefController {

    @Autowired
    private DailyMenuService dailyMenuService;

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderRepository orderRepository;

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @GetMapping({"", "/"})
    public String dashboard(Model model) {
        model.addAttribute("todayMenu",
            dailyMenuService.findByDate(LocalDate.now()).orElse(null));
        model.addAttribute("pendingOrders",
            orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(o -> o.getStatus() == OrderStatus.RECIBIDO
                          || o.getStatus() == OrderStatus.EN_PREPARACION)
                .count());
        return "chef/dashboard";
    }

    // ── Menú Diario ───────────────────────────────────────────────────────────

    @GetMapping("/menu-diario")
    public String menuDiario(@RequestParam(required = false) String fecha, Model model) {
        LocalDate date = (fecha != null && !fecha.isBlank())
            ? LocalDate.parse(fecha)
            : LocalDate.now();

        DailyMenuFormDto form = dailyMenuService.findByDate(date)
            .map(dailyMenuService::toFormDto)
            .orElseGet(() -> {
                DailyMenuFormDto dto = new DailyMenuFormDto();
                dto.setDate(date);
                return dto;
            });

        model.addAttribute("form", form);
        model.addAttribute("products", productService.findAllActive());
        model.addAttribute("selectedDate", date);
        return "chef/menu-diario";
    }

    @PostMapping("/menu-diario")
    public String saveMenuDiario(@ModelAttribute("form") DailyMenuFormDto form,
                                 Model model,
                                 RedirectAttributes redirectAttrs) {
        try {
            dailyMenuService.saveMenu(form);
            redirectAttrs.addFlashAttribute("successMsg",
                "Menú del " + form.getDate() + " publicado correctamente.");
            return "redirect:/chef/menu-diario?fecha=" + form.getDate();
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMsg", e.getMessage());
            model.addAttribute("products", productService.findAllActive());
            model.addAttribute("selectedDate", form.getDate());
            return "chef/menu-diario";
        }
    }

    // ── Gestión de Pedidos ────────────────────────────────────────────────────

    @GetMapping("/pedidos")
    public String pedidos(Model model) {
        model.addAttribute("orders",
            orderRepository.findAllByOrderByCreatedAtDesc());
        model.addAttribute("statuses", OrderStatus.values());
        return "chef/pedidos";
    }

    @PostMapping("/pedidos/{id}/estado")
    public String updateEstado(@PathVariable Long id,
                               @RequestParam OrderStatus status,
                               RedirectAttributes redirectAttrs) {
        orderRepository.findById(id).ifPresentOrElse(order -> {
            order.setStatus(status);
            orderRepository.save(order);
            redirectAttrs.addFlashAttribute("successMsg",
                "Pedido " + order.getOrderNumber() + " actualizado a: " + status);
        }, () -> redirectAttrs.addFlashAttribute("errorMsg", "Pedido no encontrado"));
        return "redirect:/chef/pedidos";
    }
}
