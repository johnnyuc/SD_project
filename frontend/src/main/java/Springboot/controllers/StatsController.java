package Springboot.controllers;

import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import Server.Controller.Objects.Stats;

@Controller
public class StatsController {

    @SendTo("/topic/update-stats")
    @PostMapping("/trigger-stats")
    public Stats triggerStats(Stats stats) {
        return stats;
    }

    @GetMapping("/stats")
    public String stats() {
        return "stats";
    }
}
