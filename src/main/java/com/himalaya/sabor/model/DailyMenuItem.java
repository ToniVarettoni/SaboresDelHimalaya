package com.himalaya.sabor.model;

import jakarta.persistence.*;

@Entity
@Table(name = "daily_menu_items")
public class DailyMenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_menu_id", nullable = false)
    private DailyMenu dailyMenu;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = true)
    private Product product;

    private String customName;

    @Column(columnDefinition = "TEXT")
    private String customDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseType courseType;

    public DailyMenuItem() {
    }

    public DailyMenuItem(DailyMenu dailyMenu, Product product, CourseType courseType) {
        this.dailyMenu = dailyMenu;
        this.product = product;
        this.courseType = courseType;
    }

    public DailyMenuItem(DailyMenu dailyMenu, String customName, String customDescription, CourseType courseType) {
        this.dailyMenu = dailyMenu;
        this.customName = customName;
        this.customDescription = customDescription;
        this.courseType = courseType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DailyMenu getDailyMenu() {
        return dailyMenu;
    }

    public void setDailyMenu(DailyMenu dailyMenu) {
        this.dailyMenu = dailyMenu;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public String getCustomName() {
        return customName;
    }

    public void setCustomName(String customName) {
        this.customName = customName;
    }

    public String getCustomDescription() {
        return customDescription;
    }

    public void setCustomDescription(String customDescription) {
        this.customDescription = customDescription;
    }

    public CourseType getCourseType() {
        return courseType;
    }

    public void setCourseType(CourseType courseType) {
        this.courseType = courseType;
    }

    public String getDisplayName() {
        if (product != null) {
            return product.getName();
        }
        return customName;
    }

    public String getDisplayDescription() {
        if (product != null) {
            return product.getDescription();
        }
        return customDescription;
    }
}
