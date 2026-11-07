package com.himalaya.sabor.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO para el formulario completo del menú diario (P5 del flujo).
 * Contiene la fecha + 8 ítems: 3 entrantes, 3 principales, 2 postres.
 */
public class DailyMenuFormDto {

    private LocalDate date;

    // 3 entrantes
    private List<DailyMenuItemDto> entrantes = new ArrayList<>();
    // 3 principales
    private List<DailyMenuItemDto> principales = new ArrayList<>();
    // 2 postres
    private List<DailyMenuItemDto> postres = new ArrayList<>();

    public DailyMenuFormDto() {
        // Inicializar con slots vacíos para el binding de Thymeleaf
        for (int i = 0; i < 3; i++) {
            DailyMenuItemDto e = new DailyMenuItemDto();
            e.setCourseType(com.himalaya.sabor.model.CourseType.ENTRANTE);
            entrantes.add(e);

            DailyMenuItemDto p = new DailyMenuItemDto();
            p.setCourseType(com.himalaya.sabor.model.CourseType.PRINCIPAL);
            principales.add(p);
        }
        for (int i = 0; i < 2; i++) {
            DailyMenuItemDto po = new DailyMenuItemDto();
            po.setCourseType(com.himalaya.sabor.model.CourseType.POSTRE);
            postres.add(po);
        }
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public List<DailyMenuItemDto> getEntrantes() { return entrantes; }
    public void setEntrantes(List<DailyMenuItemDto> entrantes) { this.entrantes = entrantes; }

    public List<DailyMenuItemDto> getPrincipales() { return principales; }
    public void setPrincipales(List<DailyMenuItemDto> principales) { this.principales = principales; }

    public List<DailyMenuItemDto> getPostres() { return postres; }
    public void setPostres(List<DailyMenuItemDto> postres) { this.postres = postres; }
}
