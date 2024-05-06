package Springboot.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SearchController {

    @GetMapping("/")
    public String redirect() {
        return "redirect:/search";
    }

    @GetMapping("/search")
    public String search() {
        return "search";
    }
}
