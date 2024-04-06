package Server.IndexStorageBarrel.Operations;

import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import Logger.LogUtil;

import Server.Controller.RMIGateway.RMIGateway;
import Server.Controller.RMIGateway.RMIGatewayInterface;
import Server.IndexStorageBarrel.IndexStorageBarrel;

/**
 * The BarrelPinger class is responsible for periodically pinging the RMIGateway
 * to indicate that the
 * IndexStorageBarrel is still active and available.
 */
public class BarrelPinger implements Runnable {
    public static int PING_INTERVAL = 5000;
    private volatile boolean running = true;
    private RMIGatewayInterface rmiGateway;
    private final IndexStorageBarrel barrel;

    /**
     * Constructs a BarrelPinger object with the specified IndexStorageBarrel.
     * 
     * @param barrel the IndexStorageBarrel to ping the RMIGateway for
     */
    public BarrelPinger(IndexStorageBarrel barrel) {
        this.barrel = barrel;
        try {
            rmiGateway = (RMIGatewayInterface) Naming
                    .lookup("rmi://" + barrel.getGatewayAddress() + ":" + RMIGateway.PORT + "/"
                            + RMIGateway.REMOTE_REFERENCE_NAME);
        } catch (RemoteException | NotBoundException | MalformedURLException e) {
            LogUtil.logError(Logger.LogUtil.ANSI_WHITE, BarrelPinger.class, e);
        }
        // Add ctrl+c shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        new Thread(this, "Barrel Pinger").start();
    }

    /**
     * Runs the BarrelPinger thread. Periodically pings the RMIGateway and handles
     * any exceptions that may occur.
     */
    @Override
    public void run() {
        while (running) {
            try {
                Thread.sleep(PING_INTERVAL);
                pingGateway();
            } catch (InterruptedException | RemoteException | NotBoundException
                    | MalformedURLException | UnknownHostException e) {
                LogUtil.logError(LogUtil.ANSI_RED, BarrelPinger.class, e);
            }
        }
        stop();
    }

    /**
     * Pings the RMIGateway to indicate that the IndexStorageBarrel is still active
     * and available.
     * 
     * @throws RemoteException       if a remote exception occurs
     * @throws NotBoundException     if the RMIGateway is not bound
     * @throws MalformedURLException if the URL is malformed
     * @throws UnknownHostException  if the host is unknown
     */
    private void pingGateway() throws RemoteException, NotBoundException, MalformedURLException, UnknownHostException {
        LogUtil.logInfo(Logger.LogUtil.ANSI_BLUE, BarrelPinger.class, "Pinging gateway.");
        rmiGateway.receivePing(barrel.getBarrelID(), barrel.getBarrelAddress());
    }

    public int getActiveBarrels() throws RemoteException {
        return rmiGateway.getActiveBarrels();
    }

    /**
     * Stops the BarrelPinger thread and removes the IndexStorageBarrel from the
     * RMIGateway.
     */
    private void stop() {
        running = false;
        try {
            rmiGateway.removeBarrel(barrel.getBarrelID());
        } catch (RemoteException e) {
            LogUtil.logError(LogUtil.ANSI_RED, BarrelPinger.class, e);
        }
    }

    /**
     * Sets the running state of the BarrelPinger thread.
     * 
     * @param running the running state to set
     */
    public void setRunning(boolean running) {
        this.running = running;
    }
}
