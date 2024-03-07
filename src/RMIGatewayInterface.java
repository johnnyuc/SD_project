import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIGatewayInterface extends Remote {
    public void sendMessage(String message) throws RemoteException;
}
