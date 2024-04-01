package Server.IndexStorageBarrel.Operations;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import Logger.LogUtil;

import Server.Controller.RMIGateway.BarrelTimestamp;
import Server.Controller.RMIGateway.RMIGateway;
import Server.Controller.RMIGateway.RMIGatewayInterface;

public class BarrelPinger implements Runnable {
    public static int PING_INTERVAL = 5000;
    private volatile boolean running = true;
    private RMIGatewayInterface rmiGateway;
    private final int barrelID;

    public BarrelPinger(int barrelID) {
        this.barrelID = barrelID;
        try {
            rmiGateway = (RMIGatewayInterface) LocateRegistry.getRegistry(RMIGateway.PORT)
                    .lookup(RMIGateway.REMOTE_REFERENCE_NAME);
        } catch (RemoteException | NotBoundException e) {
            LogUtil.logError(Logger.LogUtil.ANSI_WHITE, BarrelPinger.class, e);
        }
        // Add ctrl+c shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> running = false));
        new Thread(this, "Barrel Pinger").start();
    }

    @Override
    public void run() {
        try {
            while (running) {
                pingGateway();
                Thread.sleep(PING_INTERVAL);
            }
        } catch (InterruptedException | RemoteException | NotBoundException e) {
            LogUtil.logError(LogUtil.ANSI_RED, BarrelPinger.class, e);
        } finally {
            LogUtil.logInfo(Logger.LogUtil.ANSI_WHITE, BarrelPinger.class, "Shutting down Barrel Pinger.");
        }
    }

    private void pingGateway() throws RemoteException, NotBoundException {
        LogUtil.logInfo(Logger.LogUtil.ANSI_WHITE, BarrelPinger.class, "Pinging gateway.");
        rmiGateway.receivePing(barrelID, System.currentTimeMillis());
    }
}
