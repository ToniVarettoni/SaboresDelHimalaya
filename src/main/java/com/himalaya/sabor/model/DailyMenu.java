package com.himalaya.sabor.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "daily_menus")
public class DailyMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate date;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price = new BigDecimal("15.00");

    @Column(nullable = false)
    private boolean isPublished = false;

    @OneToMany(mappedBy = "dailyMenu", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DailyMenuItem> items = new ArrayList<>();

    public DailyMenu() {
    }

    public DailyMenu(LocalDate date, boolean isPublished) {
        this.date = date;
        this.price = new BigDecimal("15.00");
        this.isPublished = isPublished;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public boolean isPublished() {
        return isPublished;
    }

    public void setPublished(boolean published) {
        isPublished = published;
    }

    public List<DailyMenuItem> getItems() {
        return items;
    }

    public void setItems(List<DailyMenuItem> items) {
        this.items = items;
    }

    public void addItem(DailyMenuItem item) {
        items.add(item);
        item.setDailyMenu(this);
    }

    public void removeItem(DailyMenuItem item) {
        items.remove(item);
        item.setDailyMenu(null);
    }
}
