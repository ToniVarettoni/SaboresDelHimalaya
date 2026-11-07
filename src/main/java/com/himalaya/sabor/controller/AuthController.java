package com.himalaya.sabor.controller;

import com.himalaya.sabor.dto.RegisterDto;
import com.himalaya.sabor.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);
        if (("http".equals(scheme) && serverPort != 80) || ("https".equals(scheme) && serverPort != 443)) {
            url.append(":").append(serverPort);
        }
        url.append(request.getContextPath());
        return url.toString();
    }

    // P2 - Formulario de Registro de Cliente (US1)
    @GetMapping("/registro")
    public String showRegisterForm(Model model) {
        if (!model.containsAttribute("registerDto")) {
            model.addAttribute("registerDto", new RegisterDto());
        }
        return "auth/registro";
    }

    @PostMapping("/registro")
    public String registerUser(@Valid @ModelAttribute("registerDto") RegisterDto registerDto,
                               BindingResult bindingResult,
                               HttpServletRequest request,
                               Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/registro";
        }

        if (userService.emailExists(registerDto.getEmail())) {
            model.addAttribute("errorMessage", "El correo electrónico ya está registrado en el sistema");
            return "auth/registro";
        }

        try {
            String baseUrl = getBaseUrl(request);
            userService.registerUser(registerDto, baseUrl);
            model.addAttribute("registrationSuccess", true);
            model.addAttribute("registeredEmail", registerDto.getEmail());
            return "auth/registro-exito";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/registro";
        }
    }

    // P3 - Confirmación y Reenvío de Activación de Cuenta (US2)
    @GetMapping("/activar-cuenta")
    public String activateAccount(@RequestParam(name = "token", required = false) String token, Model model) {
        UserService.ActivationResult result = userService.activateAccount(token);
        model.addAttribute("success", result.isSuccess());
        model.addAttribute("message", result.getMessage());
        return "auth/activar-cuenta";
    }

    @PostMapping("/reenviar-activacion")
    public String resendActivation(@RequestParam("email") String email,
                                   HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) {
        String baseUrl = getBaseUrl(request);
        boolean resent = userService.resendActivation(email, baseUrl);
        if (resent) {
            redirectAttributes.addFlashAttribute("resendSuccess",
                    "Se ha enviado un nuevo enlace de activación a su correo electrónico. Por favor, revise su bandeja de entrada.");
        } else {
            redirectAttributes.addFlashAttribute("resendInfo",
                    "Si el correo corresponde a una cuenta pendiente de activación, se ha enviado un nuevo enlace.");
        }
        return "redirect:/activar-cuenta";
    }

    // P4 - Formulario de Inicio de Sesión (Login) (US3)
    @GetMapping("/login")
    public String showLoginForm(@RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "unactivated", required = false) String unactivated,
                                @RequestParam(value = "logout", required = false) String logout,
                                Model model) {
        if (unactivated != null) {
            model.addAttribute("errorMessage", "La cuenta aún no ha sido activada. Revise su correo electrónico");
        } else if (error != null) {
            model.addAttribute("errorMessage", "Credenciales de acceso incorrectas");
        }

        if (logout != null) {
            model.addAttribute("infoMessage", "Sesión cerrada correctamente");
        }

        return "auth/login";
    }
}
