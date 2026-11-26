package com.himalaya.sabor;

import com.himalaya.sabor.model.CourseType;
import com.himalaya.sabor.model.DailyMenu;
import com.himalaya.sabor.model.DailyMenuItem;
import com.himalaya.sabor.repository.DailyMenuRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MenuDiarioClienteIntegrationTests {

    private static final String AVISO = "La opción de menú diario no está disponible para la fecha seleccionada";

    @Autowired private MockMvc mockMvc;
    @Autowired private DailyMenuRepository dailyMenuRepository;

    private void borrarMenuDeHoy() {
        dailyMenuRepository.findByDate(LocalDate.now()).ifPresent(dailyMenuRepository::delete);
    }

    @Test
    @DisplayName("US7 (CA7.1): la carta muestra la fecha, el precio de 15 € y los platos del menú del día")
    void cartaShowsTodayMenu() throws Exception {
        borrarMenuDeHoy();
        DailyMenu menu = new DailyMenu(LocalDate.now(), true);
        String[][] platos = {{"Entrante Uno", "ENTRANTE"}, {"Entrante Dos", "ENTRANTE"}, {"Entrante Tres", "ENTRANTE"},
                {"Principal Uno", "PRINCIPAL"}, {"Principal Dos", "PRINCIPAL"}, {"Principal Tres", "PRINCIPAL"},
                {"Postre Uno", "POSTRE"}, {"Postre Dos", "POSTRE"}};
        for (String[] pl : platos) {
            menu.addItem(new DailyMenuItem(menu, pl[0], "Plato de prueba", CourseType.valueOf(pl[1])));
        }
        dailyMenuRepository.save(menu);

        mockMvc.perform(get("/carta"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))))
                .andExpect(content().string(containsString("15.00 €")))
                .andExpect(content().string(containsString("Entrante Tres")))
                .andExpect(content().string(containsString("Principal Dos")))
                .andExpect(content().string(containsString("Postre Uno")))
                .andExpect(content().string(not(containsString(AVISO))));
    }

    @Test
    @DisplayName("US7 (CA7.2): sin menú configurado para hoy se muestra el aviso literal")
    void cartaShowsNoticeWithoutMenu() throws Exception {
        borrarMenuDeHoy();
        mockMvc.perform(get("/carta"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(AVISO)));
    }
}
