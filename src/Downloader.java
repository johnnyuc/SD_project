import java.net.URL;

/**
 * Downloader
 */
public class Downloader implements Runnable {
    private int id;

    public Downloader(int id) {
        this.id = id;
    }

    public void run() {
        System.out.println("Miauu " + id);
    }

    private void visitURL(URL url) {

    }
}