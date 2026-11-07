package com.himalaya.sabor.dto;

import com.himalaya.sabor.model.CourseType;

/**
 * DTO que representa una fila del formulario del menú diario.
 * El chef puede elegir un producto existente (productId) o ingresar un plato nuevo (customName/customDescription).
 */
public class DailyMenuItemDto {

    private Long productId;         // null si es plato nuevo
    private String customName;       // solo si productId == null
    private String customDescription;
    private CourseType courseType;   // ENTRANTE | PRINCIPAL | POSTRE

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getCustomName() { return customName; }
    public void setCustomName(String customName) { this.customName = customName; }

    public String getCustomDescription() { return customDescription; }
    public void setCustomDescription(String customDescription) { this.customDescription = customDescription; }

    public CourseType getCourseType() { return courseType; }
    public void setCourseType(CourseType courseType) { this.courseType = courseType; }
}
