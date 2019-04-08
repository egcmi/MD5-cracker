package assignment3.client.old;

import java.rmi.Remote;
import java.util.Map;

public interface ClientsSync extends Remote {

    public void startHackingRemote(Map<String, String> otherClients, byte[] currProblem, int currProblemSize) throws Exception;

    public void joinedHack(String clientIp, String clientName)  throws Exception;

    public void clientJoined(Map<String, String> newOtherClients) throws Exception;

    public int getNextNumberToTest() throws Exception;

    public void makeClientsExit() throws Exception;


}
