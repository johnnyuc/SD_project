package Server.Controller;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Server.Controller.RMIGateway
 */
public class RMIGateway extends UnicastRemoteObject implements RMIGatewayInterface {
    public static void main(String[] args) {
        try {
            System.out.println("Starting RMI Gateway...");
            RMIGateway rmiGateway = new RMIGateway();
            Registry registry = LocateRegistry.createRegistry(6001);
            registry.rebind("rmigateway", rmiGateway);
            System.out.println("RMI Gateway ready.");
        } catch (RemoteException re) {
            // TODO: Treat exception better
            System.out.println("Exception in Server.Controller.RMIGateway.main: " + re);
        }
    }

    RMIGateway() throws RemoteException {
        super();
    }

    public void sendMessage(String message) throws RemoteException {
        System.out.println("Got message: " + message);
    }
}