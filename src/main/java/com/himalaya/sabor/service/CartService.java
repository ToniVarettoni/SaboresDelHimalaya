package com.himalaya.sabor.service;

import com.himalaya.sabor.model.CartItem;
import com.himalaya.sabor.model.CartItem.ItemType;
import com.himalaya.sabor.model.DailyMenuItem;
import com.himalaya.sabor.model.Product;
import com.himalaya.sabor.repository.DailyMenuItemRepository;
import com.himalaya.sabor.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Session‑scoped cart service. Holds the list of {@link CartItem} objects for the
 * current HTTP session. No persistence – the cart lives only while the session
 * exists.
 */
@Service
public class CartService {

    private final List<CartItem> items = new ArrayList<>();

    @Autowired
    private ProductRepository productRepo;

    @Autowired
    private DailyMenuItemRepository dailyMenuItemRepo;

    public List<CartItem> getItems() {
        return new ArrayList<>(items);
    }

    public void clear() {
        items.clear();
    }

    public void addProduct(Long productId, int quantity) {
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        // Busca si ya está en el carrito
        Optional<CartItem> existing = items.stream()
                .filter(i -> i.getType() == ItemType.PRODUCT && productId.equals(i.getProductId()))
                .findFirst();
        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + quantity);
        } else {
            CartItem item = new CartItem(ItemType.PRODUCT, productId, null,
                    product.getName(), product.getPrice(), quantity);
            items.add(item);
        }
    }

    /**
     * Adds a DailyMenuItem to the cart. The caller must ensure the strict rule of
     * 3 entrantes, 3 principales and 2 postres. This method only records the item.
     */
    public void addDailyMenuItem(Long dailyMenuItemId, int quantity) {
        DailyMenuItem dmi = dailyMenuItemRepo.findById(dailyMenuItemId)
                .orElseThrow(() -> new IllegalArgumentException("Ítem de menú diario no encontrado"));
        Optional<CartItem> existing = items.stream()
                .filter(i -> i.getType() == ItemType.DAILY_MENU && dailyMenuItemId.equals(i.getDailyMenuItemId()))
                .findFirst();
        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + quantity);
        } else {
            String name = dmi.getCustomName() != null ? dmi.getCustomName() : dmi.getProduct().getName();
            BigDecimal price = dmi.getDailyMenu().getPrice(); // price fixed 15.00€
            CartItem item = new CartItem(ItemType.DAILY_MENU, null, dailyMenuItemId, name, price, quantity);
            items.add(item);
        }
    }

    public void updateQuantity(String cartItemId, int newQuantity) {
        items.stream()
                .filter(i -> i.getId().equals(cartItemId))
                .findFirst()
                .ifPresent(i -> i.setQuantity(newQuantity));
    }

    public void removeItem(String cartItemId) {
        items.removeIf(i -> i.getId().equals(cartItemId));
    }

    public BigDecimal getTotal() {
        return items.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
