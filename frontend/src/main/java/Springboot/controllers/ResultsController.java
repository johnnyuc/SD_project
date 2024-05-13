package Springboot.controllers;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.List;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import Server.Controller.RMIGateway;
import Server.Controller.RMIGatewayInterface;
import Springboot.hackernews.HackerNews;
import Springboot.util.Message;
import Springboot.openai.OpenAI;

@Controller
public class ResultsController {

    @GetMapping("/results")
    public String results(@RequestParam(name = "query", required = true) String query,
            @RequestParam(name = "page", required = false, defaultValue = "1") int page,
            Model model) {
        // TODO: How to set the value of the gatewayAddress??????
        try {
            RMIGatewayInterface rmiGateway = (RMIGatewayInterface) Naming
                    .lookup("rmi://localhost:" + RMIGateway.PORT + "/" + RMIGateway.REMOTE_REFERENCE_NAME);

            model.addAttribute("searchResults", rmiGateway.searchQuery(query, page));

            System.out.println("Contextualized analysis: " + OpenAI.getContextualizedAnalysis(query));
        } catch (MalformedURLException | RemoteException | NotBoundException e) {
            return "redirect:/error";
        }

        return "results";
    }

    @MessageMapping("/index-top-stories")
    private void onIndexHackerNewsTopStoriesPress(Message request) {
        try {
            RMIGatewayInterface rmiGateway = (RMIGatewayInterface) Naming
                    .lookup("rmi://localhost:" + RMIGateway.PORT + "/" + RMIGateway.REMOTE_REFERENCE_NAME);
            List<String> topStories = HackerNews.getTopStories(request.content());

            System.out.println("Top stories: " + topStories);

            for (String story : topStories)
                rmiGateway.priorityEnqueueURL(story);
        } catch (MalformedURLException | RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }
}
