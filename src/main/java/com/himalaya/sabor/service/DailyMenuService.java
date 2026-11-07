package com.himalaya.sabor.service;

import com.himalaya.sabor.dto.DailyMenuFormDto;
import com.himalaya.sabor.dto.DailyMenuItemDto;
import com.himalaya.sabor.model.*;
import com.himalaya.sabor.repository.DailyMenuRepository;
import com.himalaya.sabor.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DailyMenuService {

    @Autowired
    private DailyMenuRepository dailyMenuRepo;

    @Autowired
    private ProductRepository productRepo;

    /** Devuelve el menú publicado para hoy, o vacío */
    public Optional<DailyMenu> findTodayMenu() {
        return dailyMenuRepo.findByDateAndIsPublishedTrue(LocalDate.now());
    }

    /** Devuelve el menú (publicado o no) para una fecha concreta */
    public Optional<DailyMenu> findByDate(LocalDate date) {
        return dailyMenuRepo.findByDate(date);
    }

    /**
     * Convierte un DailyMenu existente en un DailyMenuFormDto para pre-rellenar el formulario de edición.
     */
    public DailyMenuFormDto toFormDto(DailyMenu menu) {
        DailyMenuFormDto dto = new DailyMenuFormDto();
        dto.setDate(menu.getDate());

        List<DailyMenuItemDto> entrantes = new ArrayList<>();
        List<DailyMenuItemDto> principales = new ArrayList<>();
        List<DailyMenuItemDto> postres = new ArrayList<>();

        for (DailyMenuItem item : menu.getItems()) {
            DailyMenuItemDto d = new DailyMenuItemDto();
            d.setCourseType(item.getCourseType());
            if (item.getProduct() != null) {
                d.setProductId(item.getProduct().getId());
            } else {
                d.setCustomName(item.getCustomName());
                d.setCustomDescription(item.getCustomDescription());
            }
            switch (item.getCourseType()) {
                case ENTRANTE  -> entrantes.add(d);
                case PRINCIPAL -> principales.add(d);
                case POSTRE    -> postres.add(d);
            }
        }

        // Rellenar slots vacíos si faltan ítems (para mantener 3-3-2 en el form)
        while (entrantes.size() < 3) {
            DailyMenuItemDto d = new DailyMenuItemDto();
            d.setCourseType(CourseType.ENTRANTE);
            entrantes.add(d);
        }
        while (principales.size() < 3) {
            DailyMenuItemDto d = new DailyMenuItemDto();
            d.setCourseType(CourseType.PRINCIPAL);
            principales.add(d);
        }
        while (postres.size() < 2) {
            DailyMenuItemDto d = new DailyMenuItemDto();
            d.setCourseType(CourseType.POSTRE);
            postres.add(d);
        }

        dto.setEntrantes(entrantes);
        dto.setPrincipales(principales);
        dto.setPostres(postres);
        return dto;
    }

    /**
     * Valida y persiste (o actualiza) el menú diario.
     * Regla estricta CA6.3: exactamente 3 entrantes, 3 principales, 2 postres con nombre no vacío.
     *
     * @throws IllegalArgumentException si la validación falla
     */
    @Transactional
    public DailyMenu saveMenu(DailyMenuFormDto dto) {
        // Validar regla 3-3-2
        validateItems(dto.getEntrantes(), 3, "entrantes");
        validateItems(dto.getPrincipales(), 3, "platos principales");
        validateItems(dto.getPostres(), 2, "postres");

        // Buscar si ya existe un menú para esa fecha
        DailyMenu menu = dailyMenuRepo.findByDate(dto.getDate())
                .orElse(new DailyMenu());

        menu.setDate(dto.getDate());
        menu.setPrice(new java.math.BigDecimal("15.00"));
        menu.setPublished(true);

        // Limpiar ítems anteriores (orphanRemoval se encarga de la BD)
        menu.getItems().clear();

        // Añadir los nuevos
        addItems(menu, dto.getEntrantes(), CourseType.ENTRANTE);
        addItems(menu, dto.getPrincipales(), CourseType.PRINCIPAL);
        addItems(menu, dto.getPostres(), CourseType.POSTRE);

        return dailyMenuRepo.save(menu);
    }

    // ── Helpers privados ─────────────────────────────────────────────────────

    private void validateItems(List<DailyMenuItemDto> items, int required, String label) {
        long filled = items.stream().filter(this::hasName).count();
        if (filled != required) {
            throw new IllegalArgumentException(
                "Debe completar exactamente " + required + " " + label +
                " (completados: " + filled + ")");
        }
    }

    private boolean hasName(DailyMenuItemDto d) {
        if (d.getProductId() != null) return true;
        return d.getCustomName() != null && !d.getCustomName().isBlank();
    }

    private void addItems(DailyMenu menu, List<DailyMenuItemDto> dtos, CourseType type) {
        for (DailyMenuItemDto d : dtos) {
            if (!hasName(d)) continue;
            DailyMenuItem item = new DailyMenuItem();
            item.setCourseType(type);
            if (d.getProductId() != null) {
                productRepo.findById(d.getProductId()).ifPresent(item::setProduct);
            } else {
                item.setCustomName(d.getCustomName().trim());
                item.setCustomDescription(
                    d.getCustomDescription() != null ? d.getCustomDescription().trim() : null);
            }
            menu.addItem(item);
        }
    }
}
