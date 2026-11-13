package com.himalaya.sabor.repository;

import com.himalaya.sabor.model.CourseType;
import com.himalaya.sabor.model.DailyMenu;
import com.himalaya.sabor.model.DailyMenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailyMenuItemRepository extends JpaRepository<DailyMenuItem, Long> {
    List<DailyMenuItem> findByDailyMenu(DailyMenu dailyMenu);
    List<DailyMenuItem> findByDailyMenuAndCourseType(DailyMenu dailyMenu, CourseType courseType);

    /** ¿El producto está en algún menú diario de hoy o de una fecha posterior? (D-13) */
    boolean existsByProductIdAndDailyMenuDateGreaterThanEqual(Long productId, LocalDate date);
}
