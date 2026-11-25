package com.himalaya.sabor.controller;

import com.himalaya.sabor.dto.DailyMenuFormDto;
import com.himalaya.sabor.model.CourseType;
import com.himalaya.sabor.model.Order;
import com.himalaya.sabor.model.OrderStatus;
import com.himalaya.sabor.repository.OrderRepository;
import com.himalaya.sabor.service.DailyMenuService;
import com.himalaya.sabor.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Panel del Chef – protegido por ROLE_CHEF (SecurityConfig).
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
                          || o.getStatus() == OrderStatus.EN_PREPARACION
                          || o.getStatus() == OrderStatus.EN_REPARTO)
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

        populateMenuProducts(model, form.getDate());
        model.addAttribute("form", form);
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
            populateMenuProducts(model, form.getDate());
            return "chef/menu-diario";
        }
    }

    private void populateMenuProducts(Model model, LocalDate date) {
        model.addAttribute("entrantesProducts", productService.findActiveByCourseType(CourseType.ENTRANTE));
        model.addAttribute("principalesProducts", productService.findActiveByCourseType(CourseType.PRINCIPAL));
        model.addAttribute("postresProducts", productService.findActiveByCourseType(CourseType.POSTRE));
        model.addAttribute("selectedDate", date);
    }

    // ── Gestión de Pedidos ────────────────────────────────────────────────────

    @GetMapping("/pedidos")
    @Transactional(readOnly = true)
    public String pedidos(Model model) {
        LocalDate today = LocalDate.now();
        List<Order> allOrders = orderRepository.findAllByOrderByCreatedAtDesc();

        List<Order> todayOrders = allOrders.stream()
                .filter(o -> o.getCreatedAt().toLocalDate().equals(today))
                .toList();

        Map<LocalDate, List<Order>> pastOrdersByDate = allOrders.stream()
                .filter(o -> !o.getCreatedAt().toLocalDate().equals(today))
                .collect(Collectors.groupingBy(
                        o -> o.getCreatedAt().toLocalDate(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        model.addAttribute("todayDate", today);
        model.addAttribute("todayOrders", todayOrders);
        model.addAttribute("pastOrdersByDate", pastOrdersByDate);
        model.addAttribute("statuses", OrderStatus.values());
        return "chef/pedidos";
    }

    @PostMapping("/pedidos/{id}/estado")
    @Transactional
    public String updateEstado(@PathVariable Long id,
                               @RequestParam OrderStatus status,
                               RedirectAttributes redirectAttrs) {
        orderRepository.findById(id).ifPresentOrElse(order -> {
            order.setStatus(status);
            orderRepository.save(order);
            String clientName = order.getUser().getName();
            redirectAttrs.addFlashAttribute("successMsg",
                "Pedido para " + clientName + " cambió a estado " + status.getDisplayName());
        }, () -> redirectAttrs.addFlashAttribute("errorMsg", "Pedido no encontrado"));
        return "redirect:/chef/pedidos";
    }
}
