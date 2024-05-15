package Springboot.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller class to handle error-related requests.
 */
@Controller
public class ErrorController {

    /**
     * Directly returns the error page.
     *
     * @return the view name for the error page
     */
    @GetMapping("/error")
    public String error() {
        return "error";
    }
}
