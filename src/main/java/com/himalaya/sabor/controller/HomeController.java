package com.himalaya.sabor.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "redirect:/carta";
    }

    @GetMapping("/carta")
    public String carta() {
        return "carta/index";
    }
}
