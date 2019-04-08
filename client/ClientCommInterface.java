package assignment3.client;
import java.rmi.Remote;

/**
 * Interface for the server to communicate with the (master) client via RMI
 */
public interface ClientCommInterface extends Remote {

    void publishProblem(byte[] hash, int problemsize) throws Exception;

}
