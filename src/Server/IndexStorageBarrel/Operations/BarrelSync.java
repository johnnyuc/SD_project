package Server.IndexStorageBarrel.Operations;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;

import Logger.LogUtil;
import ReliableMulticast.ReliableMulticast;
import ReliableMulticast.Objects.CrawlData;
import Server.IndexStorageBarrel.Tools.SyncData;
import Server.IndexStorageBarrel.Tools.SyncRequest;

public class BarrelSync implements Runnable {
    private final BarrelPopulate barrelPopulate;
    private final BarrelRetriever barrelRetriever;
    private final ReliableMulticast reliableMulticast;
    private boolean running = true;

    public BarrelSync(BarrelPopulate barrelPopulate,
            BarrelRetriever barrelRetriever,
            ReliableMulticast reliableMulticast) {
        this.barrelPopulate = barrelPopulate;
        this.barrelRetriever = barrelRetriever;
        this.reliableMulticast = reliableMulticast;
        // add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        new Thread(this, "Barrel Sync").start();
    }

    @Override
    public void run() {
        reliableMulticast.startReceiving();
        reliableMulticast.send(getSyncRequest());
        // TODO: This only works considering 2 barrels are used. If more barrels are
        // used, this will need to be changed.
        // It wont break, but alot of useless data will be added to the barrels

        // TODO se recebe um valor enquanto está a sincronizar, ele vai ter id's
        // diferentes nas duas bds, mas será que importa?
        // O que importa é que depois eles vão estar no mesmo id no final e os dados
        // estão todos lá dentro. Solução: Meter o receiver a encher a queue mas não a
        // tirar os valores. Só tirar quando estiver sincronizado.

        // TODO Se o pedido de sincronização for perdido, a bd nunca se sincroniza
        while (running) {
            try {
                Object data = reliableMulticast.getData();
                if (data == null)
                    running = false;
                else if (data.getClass() == SyncRequest.class)
                    reliableMulticast.send(getSyncData((SyncRequest) data));
                else {
                    barrelPopulate.insertSyncData((SyncData) data);
                }
            } catch (Exception e) {
                LogUtil.logError(LogUtil.ANSI_RED, BarrelSync.class, e);
            }
        }
    }

    private SyncRequest getSyncRequest() {
        return new SyncRequest(barrelRetriever.getLastIDs());
    }

    private SyncData getSyncData(SyncRequest syncRequest) {
        SyncData syncData = new SyncData(new HashMap<>());
        for (String table : syncRequest.lastIDs().keySet()) {
            int lastID = syncRequest.lastIDs().get(table);
            ResultSet rs = barrelRetriever.getTableWithStartID(table, lastID);
            syncData.tableResults().put(table, rs);
        }

        // TODO: Este codigo está muita feio, mas funciona. São cenas bué especificas, é
        // fodido fazer uma funcao que funcione para tudo

        ResultSet rs = barrelRetriever.getWeakTableWithStartID("website_urls", syncRequest.lastIDs().get("websites"));
        syncData.tableResults().put("website_urls", rs);
        rs = barrelRetriever.getWeakTableWithStartID("website_keywords",
                syncRequest.lastIDs().get("websites"));
        syncData.tableResults().put("website_keywords", rs);

        return syncData;
    }

    private void stop() {
        reliableMulticast.stopReceiving();
        reliableMulticast.stopSending();
    }
}
