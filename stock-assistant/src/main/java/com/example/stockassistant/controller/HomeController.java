package com.example.stockassistant.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        // 로그인 후 메인 화면은 일반질의로 바로 진입
        return "redirect:/general-chat";
    }
}
