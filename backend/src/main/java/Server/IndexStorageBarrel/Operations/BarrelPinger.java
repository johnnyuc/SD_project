package Server.IndexStorageBarrel.Operations;

// Package imports
import Server.IndexStorageBarrel.IndexStorageBarrel;
import Server.Controller.RMIGateway;
import Server.Controller.RMIGatewayInterface;

// Logging imports
import Logger.LogUtil;

// General imports
import java.rmi.Naming;

// Exception imports
import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;

/**
 * The BarrelPinger class is responsible for periodically pinging the RMIGateway
 * to indicate that the
 * IndexStorageBarrel is still active and available.
 */
public class BarrelPinger implements Runnable {
    /**
     * The interval at which the RMIGateway is pinged.
     */
    public static int PING_INTERVAL = 5000;
    /**
     * A boolean value indicating whether the BarrelPinger is running.
     */
    private volatile boolean running = true;
    /**
     * The RMIGatewayInterface object used to ping the RMIGateway.
     */
    private RMIGatewayInterface rmiGateway;
    /**
     * The IndexStorageBarrel object to ping the RMIGateway for.
     */
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
            LogUtil.logInfo(Logger.LogUtil.ANSI_RED, BarrelPinger.class, "Error connecting to RMIGateway.");
            barrel.stop();
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
                LogUtil.logInfo(LogUtil.ANSI_RED, BarrelPinger.class, "Error reaching RMIGateway.");
                rmiGateway = null;
                running = false;
                barrel.stop();
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

    /**
     * Gets the number of active barrels from the RMIGateway.
     *
     * @return the number of active barrels
     * @throws RemoteException if a remote exception occurs
     */
    public int getActiveBarrels() throws RemoteException {
        return rmiGateway.getActiveBarrels();
    }

    public RMIGatewayInterface getRMIGateway() {
        return rmiGateway;
    }

    /**
     * Stops the BarrelPinger thread and removes the IndexStorageBarrel from the
     * RMIGateway.
     */
    private void stop() {
        running = false;
        if (rmiGateway == null)
            return;

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
