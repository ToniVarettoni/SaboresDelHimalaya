package com.himalaya.sabor.repository;

import com.himalaya.sabor.model.DailyMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyMenuRepository extends JpaRepository<DailyMenu, Long> {
    Optional<DailyMenu> findByDate(LocalDate date);
    Optional<DailyMenu> findByDateAndIsPublishedTrue(LocalDate date);
}
