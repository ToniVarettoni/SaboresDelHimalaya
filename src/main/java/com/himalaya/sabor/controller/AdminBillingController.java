package com.himalaya.sabor.controller;

import com.himalaya.sabor.model.Order;
import com.himalaya.sabor.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Panel de Administración: Informe de Facturación Diaria (US15 / P13).
 * Protegido para ROLE_ADMIN en SecurityConfig (/admin/**).
 */
@Controller
@RequestMapping("/admin/facturacion")
public class AdminBillingController {

    private static final DateTimeFormatter LABEL_FMT = DateTimeFormatter.ofPattern("dd/MM");

    @Autowired
    private OrderService orderService;

    @GetMapping
    public String viewBillingReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(defaultValue = "14") int dias,
            Model model) {

        LocalDate selectedDate = (fecha != null) ? fecha : LocalDate.now();
        int boundedDays = Math.max(3, Math.min(dias, 180));

        // Detalle de pedidos completados del día seleccionado (CA15.2)
        List<Order> completedOrders = orderService.getCompletedOrdersByDate(selectedDate);
        int totalOrdersCount = completedOrders.size();
        BigDecimal totalBilled = completedOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Datos históricos para el gráfico (eje X = fechas, eje Y = cantidad de pedidos)
        Map<LocalDate, Long> dailyCounts = orderService.getDailyCompletedOrderCounts(boundedDays);
        List<String> chartLabels = new ArrayList<>();
        List<String> chartIsoDates = new ArrayList<>();
        List<Long> chartCounts = new ArrayList<>();

        for (Map.Entry<LocalDate, Long> entry : dailyCounts.entrySet()) {
            chartLabels.add(entry.getKey().format(LABEL_FMT));
            chartIsoDates.add(entry.getKey().toString());
            chartCounts.add(entry.getValue());
        }

        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("dias", boundedDays);
        model.addAttribute("orders", completedOrders);
        model.addAttribute("totalOrdersCount", totalOrdersCount);
        model.addAttribute("totalBilled", totalBilled);
        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("chartIsoDates", chartIsoDates);
        model.addAttribute("chartCounts", chartCounts);

        return "admin/facturacion";
    }
}
