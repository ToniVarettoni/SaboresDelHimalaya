package com.himalaya.sabor.service;

import com.himalaya.sabor.dto.OrderConfirmDto;
import com.himalaya.sabor.model.*;
import com.himalaya.sabor.repository.OrderRepository;
import com.himalaya.sabor.repository.ProductRepository;
import com.himalaya.sabor.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

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
            // Para ítems de menú diario, itemDetailsJson guarda el nombre
            if (cartItem.getType() == CartItem.ItemType.DAILY_MENU) {
                oi.setItemDetailsJson("{\"name\":\"" + cartItem.getName() + "\"}");
            }
            order.addItem(oi);
        }

        Order saved = orderRepository.save(order);
        cartService.clear();
        return saved;
    }

    private String generateOrderNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        String candidate = "PED-" + date + "-" + random;
        // Garantizar unicidad
        while (orderRepository.findByOrderNumber(candidate).isPresent()) {
            random = ThreadLocalRandom.current().nextInt(1000, 9999);
            candidate = "PED-" + date + "-" + random;
        }
        return candidate;
    }
}
