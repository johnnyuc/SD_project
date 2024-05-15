package Springboot.controllers;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.http.HttpHeaders;

import Server.Controller.RMIGateway;
import Server.Controller.RMIGatewayInterface;
import Server.Controller.Objects.Stats;

@Controller
public class StatsController {
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public StatsController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/trigger-stats")
    public ResponseEntity<String> triggerStats(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationToken,
            @RequestBody Stats stats) {

        if (!authorizationToken.equals("someauthtokenidk"))
            return ResponseEntity.status(403).body("Unauthorized");

        messagingTemplate.convertAndSend("/topic/update-stats", stats);
        return ResponseEntity.ok("Stats updated!");
    }

    @GetMapping("/stats")
    public String stats(Model model) {
        try {
            RMIGatewayInterface rmiGateway = (RMIGatewayInterface) Naming
                    .lookup("rmi://localhost:" + RMIGateway.PORT + "/" + RMIGateway.REMOTE_REFERENCE_NAME);

            Stats stats = rmiGateway.getStats();

            model.addAttribute("barrelStats", stats.barrelStats());
            model.addAttribute("topSearches", stats.mostSearched());
        } catch (MalformedURLException | RemoteException | NotBoundException e) {
            e.printStackTrace();
            return "redirect:/error";
        }
        return "stats";
    }
}
