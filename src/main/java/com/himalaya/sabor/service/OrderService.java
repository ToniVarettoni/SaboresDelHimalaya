package com.himalaya.sabor.service;

import com.himalaya.sabor.dto.OrderConfirmDto;
import com.himalaya.sabor.dto.OrderModifyDto;
import com.himalaya.sabor.model.*;
import com.himalaya.sabor.repository.DailyMenuRepository;
import com.himalaya.sabor.repository.OrderRepository;
import com.himalaya.sabor.repository.ProductRepository;
import com.himalaya.sabor.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class OrderService {

    public static final String MSG_MODIFICAR_EXPIRADO =
            "El plazo límite de 10 minutos para modificar el pedido ha expirado. Su pedido ya está en proceso de preparación";
    public static final String MSG_CANCELAR_EXPIRADO =
            "No es posible cancelar el pedido una vez transcurridos los 10 minutos de margen";
    public static final String MSG_CANCELADO_OK =
            "Pedido cancelado correctamente sin coste";

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DailyMenuRepository dailyMenuRepository;

    @Autowired
    private EmailService emailService;

    /**
     * Convierte los ítems del carrito en un pedido persistido.
     * Genera el código único PED-YYYYMMDD-XXXX y vacía el carrito de sesión.
     */
    @Transactional
    public Order placeOrder(CartService cartService, OrderConfirmDto dto, String userEmail) {
        if (cartService.getItems().isEmpty()) {
            throw new IllegalStateException("El carrito está vacío");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Order order = new Order();
        order.setUser(user);
        order.setDeliveryAddress(dto.getDeliveryAddress());
        order.setPhone(dto.getPhone());
        order.setPaymentMethod(dto.getPaymentMethod());
        order.setNotes(dto.getNotes());
        order.setTotalAmount(cartService.getTotal());
        order.setStatus(OrderStatus.RECIBIDO);
        order.setCreatedAt(LocalDateTime.now());
        order.setOrderNumber(generateOrderNumber());

        for (CartItem cartItem : cartService.getItems()) {
            OrderItem oi = new OrderItem();
            oi.setUnitPrice(cartItem.getUnitPrice());
            oi.setQuantity(cartItem.getQuantity());
            oi.setSubtotal(cartItem.getSubtotal());

            if (cartItem.getType() == CartItem.ItemType.PRODUCT && cartItem.getProductId() != null) {
                Product product = productRepository.findById(cartItem.getProductId()).orElse(null);
                oi.setProduct(product);
            }
            if (cartItem.getType() == CartItem.ItemType.DAILY_MENU) {
                if (cartItem.getDailyMenuId() != null) {
                    oi.setDailyMenu(dailyMenuRepository.findById(cartItem.getDailyMenuId()).orElse(null));
                }
                String detalle = cartItem.getDetails() != null ? cartItem.getDetails() : "";
                oi.setItemDetailsJson("{\"name\":\"" + cartItem.getName() + "\",\"platos\":\"" + detalle.replace("\"", "'") + "\"}");
            }
            order.addItem(oi);
        }

        Order saved = orderRepository.save(order);
        cartService.clear();
        return saved;
    }

    /**
     * US11: Devuelve todo el historial de pedidos del usuario autenticado (activos e históricos).
     */
    @Transactional(readOnly = true)
    public List<Order> getOrdersByUserEmail(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        return orderRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * US11: Obtiene un pedido verificando que pertenezca al usuario autenticado.
     */
    @Transactional(readOnly = true)
    public Order getOrderForUser(Long orderId, String userEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));
        if (!order.getUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new SecurityException("No tiene permiso para acceder a este pedido");
        }
        return order;
    }

    /**
     * US12: Prepara el DTO de modificación a partir de un pedido existente.
     */
    public OrderModifyDto toModifyDto(Order order) {
        OrderModifyDto dto = new OrderModifyDto();
        dto.setDeliveryAddress(order.getDeliveryAddress());
        dto.setPhone(order.getPhone());
        dto.setNotes(order.getNotes());
        dto.setItems(order.getItems().stream()
                .map(i -> new OrderModifyDto.OrderItemModifyDto(i.getId(), i.getQuantity()))
                .toList());
        return dto;
    }

    /**
     * US12: Modifica productos, cantidades o dirección dentro de la ventana de 10 minutos y recalcula el total.
     */
    @Transactional
    public Order updateOrder(Long orderId, String userEmail, OrderModifyDto dto) {
        Order order = getOrderForUser(orderId, userEmail);

        if (!order.isWithinTenMinutes()) {
            throw new IllegalStateException(MSG_MODIFICAR_EXPIRADO);
        }
        if (order.getStatus() == OrderStatus.CANCELADO || order.getStatus() == OrderStatus.ENTREGADO) {
            throw new IllegalStateException("No es posible modificar un pedido en estado " + order.getStatus().getDisplayName());
        }

        order.setDeliveryAddress(dto.getDeliveryAddress().trim());
        order.setPhone(dto.getPhone().trim());
        order.setNotes(dto.getNotes() != null ? dto.getNotes().trim() : null);

        // Mapear nuevas cantidades por orderItemId
        Map<Long, Integer> requestedQuantities = dto.getItems().stream()
                .filter(i -> i.getOrderItemId() != null)
                .collect(Collectors.toMap(
                        OrderModifyDto.OrderItemModifyDto::getOrderItemId,
                        i -> i.getQuantity() != null ? i.getQuantity() : 0,
                        (a, b) -> b
                ));

        Iterator<OrderItem> it = order.getItems().iterator();
        while (it.hasNext()) {
            OrderItem item = it.next();
            Integer newQty = requestedQuantities.get(item.getId());
            if (newQty == null || newQty <= 0) {
                item.setOrder(null);
                it.remove();
            } else {
                item.setQuantity(newQty);
                item.setSubtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(newQty)));
            }
        }

        // Si el cliente añadió un producto adicional de la carta
        if (dto.getAddProductId() != null) {
            int addQty = (dto.getAddQuantity() != null && dto.getAddQuantity() > 0) ? dto.getAddQuantity() : 1;
            Product product = productRepository.findById(dto.getAddProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Producto seleccionado no válido"));
            if (product.getStatus() != ProductStatus.ACTIVO) {
                throw new IllegalArgumentException("El producto '" + product.getName() + "' no está disponible");
            }

            OrderItem existingProductItem = order.getItems().stream()
                    .filter(oi -> oi.getProduct() != null && oi.getProduct().getId().equals(product.getId()))
                    .findFirst()
                    .orElse(null);

            if (existingProductItem != null) {
                int combined = existingProductItem.getQuantity() + addQty;
                existingProductItem.setQuantity(combined);
                existingProductItem.setSubtotal(existingProductItem.getUnitPrice().multiply(BigDecimal.valueOf(combined)));
            } else {
                BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(addQty));
                OrderItem newItem = new OrderItem(order, product, product.getPrice(), addQty, subtotal);
                order.addItem(newItem);
            }
        }

        if (order.getItems().isEmpty()) {
            throw new IllegalArgumentException("El pedido debe contener al menos un artículo. Si desea anularlo, use la opción Cancelar Pedido.");
        }

        BigDecimal newTotal = order.getItems().stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(newTotal);

        return orderRepository.save(order);
    }

    /**
     * US13: Cancela el pedido sin coste si está dentro de los 10 minutos y envía notificación por email.
     */
    @Transactional
    public Order cancelOrder(Long orderId, String userEmail) {
        Order order = getOrderForUser(orderId, userEmail);

        if (!order.isWithinTenMinutes()) {
            throw new IllegalStateException(MSG_CANCELAR_EXPIRADO);
        }
        if (order.getStatus() == OrderStatus.CANCELADO) {
            throw new IllegalStateException("El pedido ya se encuentra cancelado");
        }
        if (order.getStatus() == OrderStatus.ENTREGADO) {
            throw new IllegalStateException("No es posible cancelar un pedido que ya ha sido entregado");
        }

        order.setStatus(OrderStatus.CANCELADO);
        Order saved = orderRepository.save(order);

        emailService.sendOrderCancellationEmail(saved.getUser(), saved);
        return saved;
    }

    private String generateOrderNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        String candidate = "PED-" + date + "-" + random;
        while (orderRepository.findByOrderNumber(candidate).isPresent()) {
            random = ThreadLocalRandom.current().nextInt(1000, 9999);
            candidate = "PED-" + date + "-" + random;
        }
        return candidate;
    }
}
