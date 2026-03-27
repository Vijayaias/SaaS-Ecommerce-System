package com.javatechie.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping
    public String index(){
        return "index";
    }

    @GetMapping("/products")
    public String products() {
        return "products";
    }

    @GetMapping("/cart")
    public String cart() {
        return "cart";
    }

    @GetMapping("/profile")
    public String profile() {
        return "profile";
    }

    @GetMapping("/logout-page")
    public String logoutPage() {
        return "logout";
    }

    @GetMapping("/success")
    public String success(){
        return "success";
    }

    @GetMapping("/cancel")
    public String cancel() {
        return "cancel";
    }
}
