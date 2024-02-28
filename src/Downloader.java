// Jsoup imports
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

// Java imports
import java.net.URL;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Downloader
 */
public class Downloader implements Runnable {
    private final int id;

    public Downloader(int id) {
        this.id = id;
    }

    public void run() {
        System.out.println("Miauu " + id);
    }

    // Public static for now. It'll take input from a queue further down the line
    public static void visitURL(URL url) {
        final Logger logger = Logger.getLogger(Downloader.class.getName());
        try {
            Document doc = Jsoup.connect(url.toString()).get();
            StringTokenizer tokens = new StringTokenizer(doc.text());
            int countTokens = 0;
            while (tokens.hasMoreElements() && countTokens++ < 100)
                System.out.println(tokens.nextToken().toLowerCase());
            Elements links = doc.select("a[href]");
            for (Element link : links)
                System.out.println(link.text() + "\n" + link.attr("abs:href") + "\n");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Erro ao tentar conectar a URL: " + url, e);
        }
    }
}