package Springboot.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller class to handle search-related requests and redirects.
 */
@Controller
public class SearchController {

    /**
     * Redirects the root URL to the search page.
     *
     * @return the redirect path to the search page
     */
    @GetMapping("/")
    public String redirect() {
        return "redirect:/search";
    }

    /**
     * Directly returns the search page.
     *
     * @return the view name for the search page
     */
    @GetMapping("/search")
    public String search() {
        return "search";
    }
}
