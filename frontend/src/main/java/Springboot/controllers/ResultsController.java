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
import Server.IndexStorageBarrel.Objects.SearchData;
import Springboot.Application;
import Springboot.hackernews.HackerNews;
import Springboot.util.Message;
import Springboot.openai.Perplexity;

@Controller
public class ResultsController {

    @GetMapping("/results")
    public String results(@RequestParam(name = "query", required = true) String query,
            @RequestParam(name = "page", required = false, defaultValue = "1") int page,
            @RequestParam(name = "urlsLinked", required = false, defaultValue = "false") boolean urlsLinked,
            Model model) {
        try {
            RMIGatewayInterface rmiGateway = (RMIGatewayInterface) Naming
                    .lookup("rmi://" + Application.gatewayAddress + ":" + RMIGateway.PORT + "/"
                            + RMIGateway.REMOTE_REFERENCE_NAME);

            List<SearchData> searchResults;
            if (!urlsLinked) {
                searchResults = rmiGateway.searchQuery(query, page);
                // Contextualized Analysis
                model.addAttribute("contextualizedAnalysis", Perplexity.getContextualizedAnalysis(query));
            } else {
                searchResults = rmiGateway.getWebsitesLinkingTo(query, page);
                model.addAttribute("contextualizedAnalysis", "Contextualized analysis unavailable for linked URLs.");
            }

            // Results
            model.addAttribute("searchResults", searchResults);
            model.addAttribute("query", query);
            model.addAttribute("urlsLinked", urlsLinked);
            // Pagination
            model.addAttribute("previousButtonDisabled", page <= 1);
            model.addAttribute("currentPage", page);
            model.addAttribute("nextButtonDisabled", searchResults.size() != 10);
        } catch (MalformedURLException | RemoteException | NotBoundException e) {
            return "redirect:/error";
        }

        return "results";
    }

    @MessageMapping("/index-top-stories")
    private void onIndexHackerNewsTopStoriesPress(Message request) {
        try {
            RMIGatewayInterface rmiGateway = (RMIGatewayInterface) Naming
                    .lookup("rmi://" + Application.gatewayAddress + ":" + RMIGateway.PORT + "/"
                            + RMIGateway.REMOTE_REFERENCE_NAME);
            List<String> topStories = HackerNews.getTopStories(request.content());

            for (String story : topStories)
                rmiGateway.priorityEnqueueURL(story);
        } catch (MalformedURLException | RemoteException | NotBoundException e) {
            System.out.println("Error indexing top stories: " + e);
        }
    }
}
