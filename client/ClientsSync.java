package assignment3.client;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Map;

/**
 * Interface for synchronisation among clients
 * @author Emanuela Giovanna Calabi - 13186
 * @author Davide Pizzirani - 13413
 * @author Ravinder Singh - 13642
 */
public interface ClientsSync extends Remote {

    public LogEntry getLastProblem() throws RemoteException;

	public void writeLog(LogEntry message) throws RemoteException;

	public Map<String,String> getClients() throws RemoteException;

}
