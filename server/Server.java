package assignment3.server;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.util.HashMap;

// Mock server provided by professor; not actual server
public class Server {

    // Determines how hard the problems are, the larger this number, the longer cracking will take
    public static final int PROBLEMSIZE = 10000000;

    public static void main(String[] args) throws Exception {

        // How many problems will be generated
        int RUNS = 10; // Integer.parseInt(args[0]);

        // In client does not succeed in contacting server due to wrong remote reference:
        // Uncomment and set server hostname to the IP external IP address of the server host
        // On some some systems, this property defaults to 127.0.0.1, the localhost address,
        // which causes problems.
        //
        // System.setProperty("java.rmi.server.hostname", "10.7.162.144");

        LocateRegistry.createRegistry(1099);
        HashMap<String,Integer> scoreMap = new HashMap<String,Integer>();
        ServerCommHandler sc = new ServerCommHandler(scoreMap);
        Naming.rebind("server", sc);

        // Sleep for 10 seconds to give clients time to register before giving the first task
        // But it is also possible to register later...
        System.out.println("Server has started...");
        Thread.sleep(10000);
        System.out.println("Server gives the first problem...");

        // Now create problems and wait until they get solved
        for (int r=1; r<=RUNS; r++) {
            System.out.println("\n  Creating Problem " + r);
            sc.createAndPublishProblem(r);
            //	wait until problem is solved...
            while (!sc.isSolved()) {Thread.sleep(2);}
        }
        System.out.println("\nTasks finished");

        // Print the results
        printScores(scoreMap);
    }

    // To print the final scores
    private static void printScores(HashMap<String, Integer> scoreMap) {
        System.out.println("\nFinal score:");
        for (String s : scoreMap.keySet())
            System.out.println("  Team: " + s + " - score: " + scoreMap.get(s));

    }

}
