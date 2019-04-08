package assignment3.server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import assignment3.client.ClientCommInterface;

// Server communication handler provided by professor; handles interaction with clients via RMI
public class ServerCommHandler extends UnicastRemoteObject implements ServerCommInterface {

    private static final long serialVersionUID = -753493834709721813L;
    private ConcurrentHashMap<String, String> myCodeMap;
    private ConcurrentHashMap<String, ClientCommInterface> myClientMap;
    private ConcurrentHashMap<String, String> myIPMap;
    HashMap<String, Integer> myScoreMap;
    String currentSolution = null;
    byte[] currentHash = null;
    boolean isSolved = false;
    MessageDigest md = null;
    List<String> oldProblems;
    int currentProblem = 1;


    public ServerCommHandler(HashMap<String, Integer> scoreMap) throws RemoteException, NoSuchAlgorithmException {
        myCodeMap = new ConcurrentHashMap<String,String>();
        myClientMap = new ConcurrentHashMap<String,ClientCommInterface>();
        myIPMap = new ConcurrentHashMap<String,String>();
        myScoreMap = scoreMap;
        oldProblems = new LinkedList<String>();
        md = MessageDigest.getInstance("MD5");
    }

    @Override
    public void register(String teamName, String teamCode, ClientCommInterface cc) throws Exception {
        System.out.println("  Registering team " + teamName);
        System.out.println("    Registration comes from host " + getClientHost()); // for testing purposes
        if (myCodeMap.containsKey(teamName)){
            System.out.println("    Team " + teamName + " already registered");}
        else {
            myCodeMap.put(teamName, teamCode);
            myClientMap.put(teamName, cc);
            myScoreMap.put(teamName,0);
            myIPMap.put(teamName,getClientHost()); // remembers IP address of team contact point
            cc.publishProblem(currentHash, Server.PROBLEMSIZE);
        }
    }


    @Override
    public void reregister(String teamName, String teamCode, ClientCommInterface cc) throws Exception {
        System.out.println("  Reregistering team " + teamName);
        if (!myCodeMap.containsKey(teamName)){
            System.out.println("    Team " + teamName + " tries to reregister without having registered before");}
        else if (!teamCode.equals(myCodeMap.get(teamName))) {
            System.out.println("    Team " + teamName + " reregisters with wrong team code");}
        else {
            System.out.println("    Team " + teamName + " reregisters with new contact point");
            myClientMap.put(teamName, cc);          // overrides old cc reference
            myIPMap.put(teamName,getClientHost());  // remembers IP address of contact point
            cc.publishProblem(currentHash, Server.PROBLEMSIZE);
        }
    }


    public void createAndPublishProblem(int run) throws Exception {
        isSolved = false;
        currentSolution = generateRandomString(Server.PROBLEMSIZE);
        currentHash = md.digest(currentSolution.getBytes());
        for  (Map.Entry<String, ClientCommInterface> entry : myClientMap.entrySet()){
            System.out.println("    Team \"" + entry.getKey() + "\" receives hash for problem " + run);
            try {
                entry.getValue().publishProblem(currentHash, Server.PROBLEMSIZE);
            }
            catch (Exception e) {System.out.println("  Could not print to team " + entry.getKey());}
        }
        System.out.println("  Created and published Problem " + run + ": " + currentSolution);
        currentProblem = run; // currentProblem stores the value of run for usage by other methods
    }


    private String generateRandomString(int max) {
        Integer random = (int)Math.round(Math.random()*max);
        return random.toString();
    }

    @Override
    public void submitSolution(String teamName, String teamCode, String sol) {
        if (!teamCode.equals(myCodeMap.get(teamName))){
            System.out.println("  Submission under name of team " + teamName + " with wrong code");
        }
        else {
            byte[] solHash = md.digest(sol.getBytes());
            if (Arrays.equals(solHash, currentHash)) {
                isSolved = true;
                oldProblems.add(sol);
                currentHash = null; // So that no client can submit this solution a second time and get points again
                myScoreMap.put(teamName, myScoreMap.get(teamName)+1) ;
                System.out.println("  *** Team " + teamName + " solved Problem " + currentProblem + " correctly ***");
            }
            else {
                if (isOldSolution(sol))
                    System.out.println("  Team " + teamName + " submitted a correct solution too late");
                else {
                    System.out.println("  Team " + teamName + " submitted an incorrect solution to Problem " + currentProblem);
                    myScoreMap.put(teamName, myScoreMap.get(teamName)-1);
                }
            }
        }
    }

    private boolean isOldSolution(String sol) {
        for (String s : oldProblems)
            if (s.equals(sol))
                return true;
        return false;
    }

    public boolean isSolved() {
        return isSolved;
    }

    @Override
    public String getTeamIP(String teamName, String teamCode) throws Exception {
        if (!teamCode.equals(myCodeMap.get(teamName))){
            System.out.println("  Request for IP address of team " + teamName + " with wrong code");
            return null;}
        else return myIPMap.get(teamName);
    }

}
