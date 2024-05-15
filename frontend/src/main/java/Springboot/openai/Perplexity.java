package Springboot.openai;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class Perplexity {
    public static String getContextualizedAnalysis(String query) {
        String completionsEndpoint = "https://api.perplexity.ai/chat/completions";
        String apiKey = "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";

        String requestBody = """
                {
                    "model": "llama-3-sonar-small-32k-chat",
                    "messages":[
                        {
                            "role": "system",
                            "content": "You are a helpfull assistant."
                        },
                        {
                            "role": "user",
                            "content": "Give a short, less than 50 tokens, and contextualized analysis about this: %s"
                        }
                    ]
                }
                """
                .formatted(query);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        // Represents the HTTP request entity
        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

        // Send the request
        ResponseEntity<PerplexityItemRecord> responseEntity;
        try {
            RestTemplate restTemplate = new RestTemplate();
            responseEntity = restTemplate.exchange(
                    completionsEndpoint,
                    HttpMethod.POST,
                    requestEntity,
                    PerplexityItemRecord.class);
        } catch (Exception e) {
            return "Error generating contextualized analysis.";
        }

        PerplexityItemRecord response = responseEntity.getBody();

        // If the response did not hit a natural stop point
        if (response == null || !response.choices().get(0).finishReason().equals("stop"))
            return "Error generating contextualized analysis.";

        // Return only the first response
        return response.choices().get(0).message().content();
    }
}
