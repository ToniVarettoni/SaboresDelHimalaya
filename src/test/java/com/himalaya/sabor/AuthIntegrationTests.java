package com.himalaya.sabor;

import com.himalaya.sabor.dto.RegisterDto;
import com.himalaya.sabor.model.Role;
import com.himalaya.sabor.model.User;
import com.himalaya.sabor.model.UserStatus;
import com.himalaya.sabor.repository.UserRepository;
import com.himalaya.sabor.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Aseguramos usuario activo para pruebas
        if (userRepository.findByEmail("cliente.activo@himalaya.com").isEmpty()) {
            User activeUser = new User(
                    "Cliente Activo",
                    "cliente.activo@himalaya.com",
                    passwordEncoder.encode("password123"),
                    "Calle Prado 5",
                    "611222333",
                    Role.CLIENT,
                    UserStatus.ACTIVA
            );
            userRepository.save(activeUser);
        }

        // Aseguramos usuario pendiente para pruebas
        if (userRepository.findByEmail("cliente.pendiente@himalaya.com").isEmpty()) {
            User pendingUser = new User(
                    "Cliente Pendiente",
                    "cliente.pendiente@himalaya.com",
                    passwordEncoder.encode("password123"),
                    "Calle Alcalá 20",
                    "622333444",
                    Role.CLIENT,
                    UserStatus.PENDIENTE_ACTIVACION
            );
            pendingUser.setActivationToken("token-test-1234");
            pendingUser.setActivationExpiresAt(LocalDateTime.now().plusHours(24));
            userRepository.save(pendingUser);
        }
    }

    // ==========================================
    // US1 - REGISTRO DE CLIENTE
    // ==========================================

    @Test
    @DisplayName("US1: GET /registro muestra el formulario de alta")
    void showRegisterForm() throws Exception {
        mockMvc.perform(get("/registro"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/registro"))
                .andExpect(content().string(containsString("Crear Cuenta de Cliente")))
                .andExpect(content().string(containsString("Crear Cuenta")));
    }

    @Test
    @DisplayName("US1: Registro exitoso crea cuenta en estado 'Pendiente de activación' y genera token")
    void registerSuccessful() throws Exception {
        mockMvc.perform(post("/registro")
                        .with(csrf())
                        .param("name", "Nuevo Usuario")
                        .param("email", "nuevo.cliente@himalaya.com")
                        .param("password", "secreto123")
                        .param("address", "Avenida América 10")
                        .param("phone", "655444333"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/registro-exito"))
                .andExpect(content().string(containsString("Pendiente de activación")));

        User created = userRepository.findByEmail("nuevo.cliente@himalaya.com").orElse(null);
        assertThat(created).isNotNull();
        assertThat(created.getStatus()).isEqualTo(UserStatus.PENDIENTE_ACTIVACION);
        assertThat(created.getActivationToken()).isNotBlank();
        assertThat(created.getActivationExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("US1: Correo duplicado muestra exactamente 'El correo electrónico ya está registrado en el sistema'")
    void registerDuplicateEmailShowsExactMessage() throws Exception {
        mockMvc.perform(post("/registro")
                        .with(csrf())
                        .param("name", "Cliente Duplicado")
                        .param("email", "cliente.activo@himalaya.com")
                        .param("password", "password123")
                        .param("address", "Calle Sol 1")
                        .param("phone", "600000000"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/registro"))
                .andExpect(content().string(containsString("El correo electrónico ya está registrado en el sistema")));
    }

    // ==========================================
    // US2 - ACTIVACIÓN Y REENVÍO
    // ==========================================

    @Test
    @DisplayName("US2: Token válido activa la cuenta y muestra 'Cuenta activada con éxito. Ya puede iniciar sesión'")
    void activateValidToken() throws Exception {
        mockMvc.perform(get("/activar-cuenta").param("token", "token-test-1234"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/activar-cuenta"))
                .andExpect(content().string(containsString("Cuenta activada con éxito. Ya puede iniciar sesión")))
                .andExpect(content().string(containsString("Ir a Iniciar Sesión")));

        User activated = userRepository.findByEmail("cliente.pendiente@himalaya.com").orElseThrow();
        assertThat(activated.getStatus()).isEqualTo(UserStatus.ACTIVA);
    }

    @Test
    @DisplayName("US2: Token inválido muestra 'Enlace de activación no válido o expirado. Puede solicitar un nuevo enlace'")
    void activateInvalidToken() throws Exception {
        mockMvc.perform(get("/activar-cuenta").param("token", "token-inexistente"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/activar-cuenta"))
                .andExpect(content().string(containsString("Enlace de activación no válido o expirado. Puede solicitar un nuevo enlace")))
                .andExpect(content().string(containsString("Solicitar nuevo enlace de activación")));
    }

    @Test
    @DisplayName("US2: Token expirado muestra 'Enlace de activación no válido o expirado. Puede solicitar un nuevo enlace'")
    void activateExpiredToken() throws Exception {
        User expiredUser = new User("Exp", "exp@himalaya.com", passwordEncoder.encode("123456"), "A", "1", Role.CLIENT, UserStatus.PENDIENTE_ACTIVACION);
        expiredUser.setActivationToken("token-expirado");
        expiredUser.setActivationExpiresAt(LocalDateTime.now().minusHours(1));
        userRepository.save(expiredUser);

        mockMvc.perform(get("/activar-cuenta").param("token", "token-expirado"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/activar-cuenta"))
                .andExpect(content().string(containsString("Enlace de activación no válido o expirado. Puede solicitar un nuevo enlace")));
    }

    @Test
    @DisplayName("US2: Reenvío de activación genera un nuevo token")
    void resendActivationToken() throws Exception {
        mockMvc.perform(post("/reenviar-activacion")
                        .with(csrf())
                        .param("email", "cliente.pendiente@himalaya.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/activar-cuenta"))
                .andExpect(flash().attributeExists("resendSuccess"));

        User user = userRepository.findByEmail("cliente.pendiente@himalaya.com").orElseThrow();
        assertThat(user.getActivationToken()).isNotEqualTo("token-test-1234");
    }

    // ==========================================
    // US3 - INICIO DE SESIÓN (LOGIN)
    // ==========================================

    @Test
    @DisplayName("US3: GET /login muestra el formulario con campos email, password y botón Entrar")
    void showLoginForm() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(content().string(containsString("Iniciar Sesión")))
                .andExpect(content().string(containsString("Entrar")))
                .andExpect(content().string(containsString("Registrarse")));
    }

    @Test
    @DisplayName("US3: Credenciales incorrectas redirigen a /login?error=true y muestran 'Credenciales de acceso incorrectas'")
    void loginBadCredentials() throws Exception {
        mockMvc.perform(formLogin("/login")
                        .user("email", "cliente.activo@himalaya.com")
                        .password("password_incorrecta"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"))
                .andExpect(unauthenticated());

        mockMvc.perform(get("/login").param("error", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Credenciales de acceso incorrectas")));
    }

    @Test
    @DisplayName("US3: Credenciales correctas con estado 'Pendiente de activación' deniega sesión y muestra 'La cuenta aún no ha sido activada. Revise su correo electrónico'")
    void loginPendingActivation() throws Exception {
        mockMvc.perform(formLogin("/login")
                        .user("email", "cliente.pendiente@himalaya.com")
                        .password("password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?unactivated=true"))
                .andExpect(unauthenticated());

        mockMvc.perform(get("/login").param("unactivated", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("La cuenta aún no ha sido activada. Revise su correo electrónico")));
    }

    @Test
    @DisplayName("US3: Credenciales correctas y estado 'Activa' autentica al cliente y redirige a la vista principal")
    void loginActiveSuccess() throws Exception {
        mockMvc.perform(formLogin("/login")
                        .user("email", "cliente.activo@himalaya.com")
                        .password("password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/carta"))
                .andExpect(authenticated().withUsername("cliente.activo@himalaya.com"));
    }

    @Test
    @DisplayName("US3: Credenciales de Administrador redirigen a /admin/carta")
    void loginAdminSuccess() throws Exception {
        mockMvc.perform(formLogin("/login")
                        .user("email", "admin@himalaya.com")
                        .password("admin123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/carta"))
                .andExpect(authenticated().withUsername("admin@himalaya.com"));
    }

    @Test
    @DisplayName("US3: Credenciales de Chef redirigen a /chef/menu-diario")
    void loginChefSuccess() throws Exception {
        mockMvc.perform(formLogin("/login")
                        .user("email", "chef@himalaya.com")
                        .password("chef123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/chef/menu-diario"))
                .andExpect(authenticated().withUsername("chef@himalaya.com"));
    }
}
