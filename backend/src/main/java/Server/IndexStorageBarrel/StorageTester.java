package Server.IndexStorageBarrel;

// Package imports
import Logger.LogUtil;
import ReliableMulticast.Objects.CrawlData;
import ReliableMulticast.ReliableMulticast;

// General imports
import java.net.URI;
import java.util.List;
import java.util.ArrayList;

// Exception imports
import java.net.MalformedURLException;

/**
 * The StorageTester class is used to send dummy data to the barrel.
 */
public class StorageTester {
    /**
     * The ReliableMulticast object used to send data to the barrel.
     */
    private static ReliableMulticast multicast;

    /**
     * The main method of the StorageTester class.
     * It sends dummy data to the barrel.
     *
     * @param args The command line arguments.
     * @throws MalformedURLException if the URL is malformed
     */
    public static void main(String[] args) throws MalformedURLException {
        if (args.length != 6) {
            System.out.println("-i <interfaceAddress> -mcast <multicastGroup> -mport <port>");
            System.exit(1);
        }

        String interfaceAddress = "";
        String multicastGroup = "";
        int port = 0;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-i":
                    interfaceAddress = args[i + 1];
                    i++;
                    break;
                case "-mcast":
                    multicastGroup = args[i + 1];
                    i++;
                    break;
                case "-mport":
                    port = Integer.parseInt(args[i + 1]);
                    i++;
                    break;
                default:
                    System.out.println("Unexpected argument: " + args[i]);
                    System.exit(1);
            }
        }

        multicast = new ReliableMulticast(interfaceAddress, multicastGroup, port, StorageTester.class, null);

        // Create dummy data
        CrawlData data1 = new CrawlData(URI.create("https://google.com").toURL(), "Google" ,
                "Google is a search engine",
                new ArrayList<>(List.of("test1", "test2", "test3", "test4", "test5")),
                new ArrayList<>(List.of(URI.create("https://google.com").toURL(),
                        URI.create("https://facebook.com").toURL(),
                        URI.create("https://twitter.com").toURL())));
        CrawlData data2 = new CrawlData(URI.create("https://facebook.com").toURL(), "Facebook" ,
                "Facebook is a social media platform",
                new ArrayList<>(List.of("test6", "test7", "test8", "test9", "test10")),
                new ArrayList<>(List.of(URI.create("https://google.com").toURL(),
                        URI.create("https://facebook.com").toURL(),
                        URI.create("https://twitter.com").toURL())));
        CrawlData data3 = new CrawlData(URI.create("https://twitter.com").toURL(), "Twitter" ,
                "Twitter is a social media platform",
                new ArrayList<>(List.of("test11", "test12", "test13", "test14", "test15")),
                new ArrayList<>(List.of(URI.create("https://google.com").toURL(),
                        URI.create("https://facebook.com").toURL(),
                        URI.create("https://twitter.com").toURL())));
        CrawlData data4 = new CrawlData(URI.create("https://youtube.com").toURL(), "YouTube" ,
                "YouTube is a video sharing platform",
                new ArrayList<>(List.of("test16", "test17", "test18", "test19", "test20", "test25", "test25", "test25")),
                new ArrayList<>(List.of(URI.create("https://google.com").toURL(),
                        URI.create("https://facebook.com").toURL(),
                        URI.create("https://twitter.com").toURL())));
        CrawlData data5 = new CrawlData(URI.create("https://instagram.com").toURL(), "Instagram" ,
                "Instagram is a photo sharing platform",
                new ArrayList<>(List.of("test21", "test22", "test23", "test24", "test25")),
                new ArrayList<>(List.of(URI.create("https://google.com").toURL(),
                        URI.create("https://facebook.com").toURL(),
                        URI.create("https://twitter.com").toURL())));
        CrawlData data6 = new CrawlData(URI.create("https://google.com").toURL(), "Google" ,
                "Google is a search ______",
                new ArrayList<>(List.of("_____", "test25", "test3", "test4", "test5")),
                new ArrayList<>(List.of(URI.create("https://google.com").toURL(),
                        URI.create("https://________.com").toURL(),
                        URI.create("https://twitter.com").toURL())));

        // Send the dummy data
        sendCrawlData(data1);
        sendCrawlData(data2);
        sendCrawlData(data3);
        sendCrawlData(data4);
        sendCrawlData(data5);
        sendCrawlData(data6);
    }

    /**
     * Sends the crawling data to the barrel.
     *
     * @param crawlData the crawling data to send
     */
    public static void sendCrawlData(CrawlData crawlData) {
        LogUtil.logInfo(LogUtil.ANSI_WHITE, StorageTester.class,
                "Sending data to barrel: " + crawlData.getUrl());

        // Send the crawling data via reliable multicast
        multicast.send(crawlData);
    }
}