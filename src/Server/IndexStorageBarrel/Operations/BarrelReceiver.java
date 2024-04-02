package Server.IndexStorageBarrel.Operations;

import java.sql.SQLException;

import Logger.LogUtil;
import ReliableMulticast.ReliableMulticast;
import ReliableMulticast.Objects.CrawlData;

public class BarrelReceiver implements Runnable {
    private final BarrelPopulate barrelPopulate;
    private final ReliableMulticast reliableMulticast;
    private boolean running = true;

    public BarrelReceiver(BarrelPopulate barrelPopulate, ReliableMulticast reliableMulticast) {
        this.barrelPopulate = barrelPopulate;
        this.reliableMulticast = reliableMulticast;
        // add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        new Thread(this, "Barrel Receiver").start();
    }

    @Override
    public void run() {
        reliableMulticast.startReceiving();
        while (running) {
            try {
                CrawlData crawlData = (CrawlData) reliableMulticast.getData();
                if (crawlData == null) {
                    running = false;
                    continue;
                }

                LogUtil.logInfo(LogUtil.ANSI_WHITE, BarrelReceiver.class, "Received data: " + crawlData.getUrl());
                barrelPopulate.insertData(crawlData);
            } catch (SQLException e) {
                LogUtil.logError(LogUtil.ANSI_RED, BarrelReceiver.class, e);
            }
        }
        LogUtil.logInfo(LogUtil.ANSI_WHITE, BarrelReceiver.class, "Barrel Receiver stopped.");
    }

    private void stop() {
        reliableMulticast.stopReceiving();
        reliableMulticast.stopSending();
    }

}
