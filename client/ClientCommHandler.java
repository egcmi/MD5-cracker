package assignment3.client;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;

import assignment3.client.Client.*;

/**
 * Receives problem from server and communicates it to all clients
 * @author Emanuela Giovanna Calabi - 13186
 * @author Davide Pizzirani - 13413
 * @author Ravinder Singh - 13642
 */
class ClientCommHandler extends UnicastRemoteObject implements ClientCommInterface {
	private static final long serialVersionUID = 1008837264497538584L;

	public byte[] currProblem = null;
	int currProblemSize = 0;
	Client client;

	public ClientCommHandler(Client client) throws RemoteException {
		this.client = client;
	}

	@Override
    public void publishProblem(byte[] hash, int problemsize) {
        if (hash==null) System.out.println("Problem is empty!");
        else System.out.println(" Client received new problem of size " + problemsize);
        if (Arrays.equals(hash, client.worker.problemHash)) {
			return;
		}
        currProblem = hash;
        currProblemSize = problemsize;
        try {
			// don't do anything if it's the same problem: added because reregister causes the server to send the same problem again
        	if (currProblem != null) {
				client.broadcast(Action.PROBLEM, currProblemSize, currProblem);
				client.worker.doWork = true;
			}
        } catch (Exception e) {
        	System.out.println("Could not write to log");
        }
    }

}
