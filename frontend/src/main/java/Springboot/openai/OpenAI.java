package Springboot.openai;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class OpenAI {
    public static String getContextualizedAnalysis(String query) {
        String completionsEndpoint = "https://api.openai.com/v1/chat/completions";
        String apiKey = "sk-proj-yrzxdOAxLotY6J8Lev4kT3BlbkFJSwc0O9b0pqBViOQSxZbx";

        String requestBody = """
                {
                    "model": "gpt-3.5-turbo-0125",
                    "messages": [
                        {"role": "system", "content": "You are a helpful assistant who generates short contextualized analysis from a given search query."},
                        {"role": "user", "content": "%s"}
                    ]
                }
                """
                .formatted(query);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        // Represents the HTTP request entity
        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

        System.out.println("Sending request to OpenAI API");

        // Send the request
        ResponseEntity<OpenAIItemRecord> responseEntity;
        try {
            RestTemplate restTemplate = new RestTemplate();
            responseEntity = restTemplate.exchange(
                    completionsEndpoint,
                    HttpMethod.POST,
                    requestEntity,
                    OpenAIItemRecord.class);
        } catch (Exception e) {
            return "Error generating contextualized analysis.";
        }

        // If the response did not hit a natural stop point
        if (!responseEntity.getBody().choices().get(0).finishReason().equals("stop"))
            return "Error generating contextualized analysis.";

        // Return only the first response
        return responseEntity.getBody().choices().get(0).message().content();
    }
}
