package Server.Downloader;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.jsoup.nodes.Document;

public class Crawling {

    /**
     * Extracts all the tokens from the document.
     *
     * @param doc the document to extract tokens from
     * @return a list of tokens extracted from the document
     */
    public static List<String> getTokens(Document doc) {
        // Tokenize the resulting document
        StringTokenizer tokens = new StringTokenizer(doc.text());
        List<String> tokenList = new ArrayList<>();

        // Store all tokens in the list
        while (tokens.hasMoreElements()) {
            String token = tokens.nextToken().toLowerCase();
            token = token.trim();
            token = token.replaceAll("[\\[\\](){}?!,.:]", "");

            // Remove single characters
            if (token.length() <= 1)
                continue;

            // If it's a number, it should contain a comma or a dot
            if (token.matches("\\d+")) {
                if (token.contains(".") || token.contains(",")) {
                    tokenList.add(token);
                }
            } else {
                tokenList.add(token);
            }
        }

        return tokenList;
    }

    /**
     * Checks if the URL is valid and reachable.
     *
     * @param url the URL to check
     * @return true if the URL is valid and reachable, false otherwise
     */
    public static boolean isValidURL(String url) {
        try {
            url = getString(url);

            HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            connection.setRequestMethod("HEAD");
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.out.println("URL not reachable or does not exist: " + url);
                return false;
            }
        } catch (IOException e) {
            System.out.println("Error checking URL: " + url);
            return false;
        }

        return true;
    }

    /**
     * Encodes the URL string to ensure that it is properly formatted.
     *
     * @param href the URL string to encode
     * @return the encoded URL string
     * @throws UnsupportedEncodingException if the encoding is not supported
     */
    public static String getString(String href) throws UnsupportedEncodingException {
        int hashIndex = href.indexOf('#');
        if (hashIndex > -1) {
            // The URL contains a fragment, so encode it
            String fragment = href.substring(hashIndex + 1);
            String encodedFragment = URLEncoder.encode(fragment, StandardCharsets.UTF_8);
            // Replace < and > characters
            encodedFragment = encodedFragment.replace("<", "%3C").replace(">", "%3E");
            href = href.substring(0, hashIndex) + '#' + encodedFragment;
        }
        return href;
    }
}
