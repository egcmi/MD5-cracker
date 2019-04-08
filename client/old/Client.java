package assignment3.client.old;

import assignment3.server.ServerCommInterface;
//import javafx.util.Pair; // TODO error on my system, will it give error on RPIs?
// ^ removed -> use HashMap

import java.net.InetAddress;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

// java -cp out/production/DS2018 assignment3.server.Server
// java -cp out/production/DS2018 assignment3.client.Client client1 2001
// java -cp out/production/DS2018 assignment3.client.Client rmi://10.7.145.175:2001/client1 client2 2002
// java -cp out/production/DS2018 assignment3.client.Client rmi://10.7.145.175:2001/client1 client3 2003

public class Client extends UnicastRemoteObject implements ClientsSync {// , Runnable {
	private static final long serialVersionUID = 1L; // TODO added because it gave error

	public final String SERVER_NAME = "rmi://localhost/server";
	public final String TEAM_NAME = "Rainbow Unicorns";
	public final String TEAM_CODE = "413642";

	public ClientCommHandler cCommHandler;
	public ServerCommInterface serverComm;

	// Map with name and ip of another RMI
	public Map<String, String> otherClients;

	public String serverAddress;
	public String clientName;
	public int clientPort;
	public String clientAddress;

	HackingThread currentThread;

	public boolean isSolutionFound = false;
	public boolean hackingStarted = false;
	public int nextNumberToTest = 0;

	/**
	 * When the client is the first to connect. It registers to the server
	 * 
	 * @param clientName
	 *            The name of this client to enable RMI
	 * @throws Exception
	 */
	public Client(String clientName, int clientPort) throws Exception {
		this.clientName = clientName;
		this.clientPort = clientPort;
		this.clientAddress = String.valueOf(InetAddress.getLocalHost()).split("/")[1] + ":" + clientPort;
		otherClients = new HashMap<>();
		otherClients.put(clientName, this.clientAddress);
		// this.currentThread = new Thread(this);

		// Client Communication Handler
		cCommHandler = new ClientCommHandler();

		// Start RMI (as server)
		System.setProperty("java.rmi.server.hostname", String.valueOf(InetAddress.getLocalHost()).split("/")[1]);
		System.setProperty("java.security.policy", "file:./security.policy");
		System.setSecurityManager(new SecurityManager());
		LocateRegistry.createRegistry(clientPort);
		Naming.bind("rmi://localhost:" + clientPort + "/" + clientName, this);

		// Register to the server
		serverComm = (ServerCommInterface) Naming.lookup(SERVER_NAME);
		//serverComm.register(TEAM_NAME, TEAM_CODE, cCommHandler); --------------------------------------------------------------------------
		System.out.println("Client registered with the server");

		// Wait until there is no other problem
		while (cCommHandler.currProblem == null) {
			Thread.sleep(1);
		}

		// Function to start hacking
		//startHack();
	}

	/**
	 *
	 * @param serverAddress
	 *            Address of the first connected client to pass the address of this
	 *            client
	 * @param clientName
	 *            The name of the client just connected
	 * @throws RemoteException
	 * @throws AlreadyBoundException
	 */
	public Client(String serverAddress, String clientName, int clientPort) throws Exception {
		System.out.println("serverAddress:" + serverAddress);
		this.clientAddress = String.valueOf(InetAddress.getLocalHost()).split("/")[1] + ":" + clientPort;
		this.serverAddress = serverAddress;
		this.clientName = clientName;
		this.clientPort = clientPort;
		otherClients = new HashMap<>();
		// this.currentThread = new HackingThread(this);

		// Client Communication Handler for later communication when winning
		cCommHandler = new ClientCommHandler();

		serverComm = (ServerCommInterface) Naming.lookup(SERVER_NAME);

		// Start RMI (as server)
		System.setProperty("java.rmi.server.hostname", String.valueOf(InetAddress.getLocalHost()).split("/")[1]);
		System.setProperty("java.security.policy", "file:./security.policy");
		System.setSecurityManager(new SecurityManager());
		LocateRegistry.createRegistry(clientPort);
		Naming.bind("rmi://:" + clientPort + "/" + clientName, this);

		// Communicate the first client that we joined hacking
		ClientsSync rmi = (ClientsSync) Naming.lookup(serverAddress);
		rmi.joinedHack(String.valueOf(InetAddress.getLocalHost()).split("/")[1] + ":" + clientPort, clientName);
	}

	/**
	 * Start hacking, executed by first client
	 */
	public void startHack() throws Exception {
	    //while(cCommHandler.currProblem == null) {
	    //    System.out.println("Waiting for problem synchronization...");
	    //    Thread.sleep(10);
        //}

		this.currentThread = new HackingThread(this);
		this.currentThread.start();
		hackingStarted = true;
		isSolutionFound = false;

		System.out.println("[startHacking()] Started hacking, communicating the other clients");

		// Start hacking on the other clients
		for (Map.Entry<String, String> p : otherClients.entrySet()) {
			if (!p.getKey().equals(this.clientName) && !p.getValue().equals(this.clientAddress)) {
				ClientsSync rmi = (ClientsSync) Naming.lookup("rmi://" + p.getValue() + "/" + p.getKey());
				rmi.startHackingRemote(otherClients, cCommHandler.currProblem, cCommHandler.currProblemSize);
				// TODO: Add number with which to start but problem with this thread
				// synchronization ???
			}
		}
	}

	/**
	 * When first client tells the other clients to start hacking
	 */
	@Override
	public void startHackingRemote(Map<String, String> otherClients, byte[] currProblem, int currProblemSize)
			throws Exception {
	    // this.nextNumberToTest = 0;
        /*while(cCommHandler.currProblem == null) {
            System.out.println("Waiting for problem synchronization...");
            Thread.sleep(10);
        }*/

        this.cCommHandler.currProblem = currProblem;
        this.cCommHandler.currProblemSize = currProblemSize;
        this.otherClients = otherClients;
		this.currentThread = new HackingThread(this);
		hackingStarted = true;

		System.out.println(clientName + "[startHackingRemote()]: arrived signal to start hack, array is: "
				+ Arrays.toString(otherClients.entrySet().toArray()));
		this.currentThread.start();
		isSolutionFound = false;

		// DO NOT REMOVE, IMPORTANT FOR SYNCHRONIZATION
		// Thread.sleep(500);
		// DO NOT REMOVE, IMPORTANT FOR SYNCHRONIZATION

	}

	public void afterHacked(boolean firstFound, Integer solution) throws Exception {
        this.nextNumberToTest = 0;
        cCommHandler.currProblem = null;
        cCommHandler.currProblemSize = 0;

        Thread.sleep(300);

	    synchronized (currentThread) {
			// Current thread take control again when hacking thread is over
			System.out.println("CIAOOOOOO");
			isSolutionFound = true;

			// Stop thread in all the other clients
			if (firstFound) {
                // Wait until a solution is found
                this.nextNumberToTest = 0;
                cCommHandler.currProblem = null;
                cCommHandler.currProblemSize = 0;

                System.out.println("Telling other clients to set is solution found...");

				for (Map.Entry<String, String> p : otherClients.entrySet()) {
					if (!p.getKey().equals(this.clientName) && !p.getValue().equals(this.clientAddress)) {
						ClientsSync rmi2 = (ClientsSync) Naming.lookup("rmi://" + p.getValue() + "/" + p.getKey());
						rmi2.makeClientsExit();
					}
				}

				/*try {
                    currentThread.stop();
                } catch(SecurityException e) {
				    System.out.println("HIII");
                }*/

				//serverComm.reregister(TEAM_NAME, TEAM_CODE, cCommHandler); -----------------------------------------------------------------------
				serverComm.submitSolution(TEAM_NAME, TEAM_CODE, solution.toString());

				//cCommHandler.currProblemSize = -1;
				//cCommHandler.currProblem = null;

                Thread.sleep(300);

				startHack();

			}

		}
	}

	public void solutionFound() throws Exception {

	}

	/**
	 * Executed only on the first client. A client that joined after the first
	 * client send the first client a message with its ip and clientname
	 */
	@Override
	public void joinedHack(String clientIp, String clientName) throws Exception {
		otherClients.put(clientName, clientIp);
		System.out.println("client " + clientIp + " " + clientName + " contacted the first client, array is: "
				+ Arrays.toString(otherClients.entrySet().toArray()));

		// If the client connected after the arrival of a problem he is messaged to
		// start hacking
		// and about the existence of the other clients
		if (hackingStarted == true) {
			ClientsSync rmi = (ClientsSync) Naming.lookup("rmi://" + clientIp + "/" + clientName);
			rmi.startHackingRemote(otherClients, cCommHandler.currProblem, cCommHandler.currProblemSize);

			// TODO: alert all the other clients about the arrival of another client
			// Alert all the other clients about the new joined client
			for (Map.Entry<String, String> p : otherClients.entrySet()) {
				if (!p.getKey().equals(clientName) && !p.getValue().equals(clientIp)
						&& !p.getKey().equals(this.clientName) && !p.getKey().equals(this.clientAddress)) {
					ClientsSync rmi2 = (ClientsSync) Naming.lookup("rmi://" + p.getValue() + "/" + p.getKey());
					rmi2.clientJoined(otherClients);
				}
			}
		}
	}

	/**
	 * A client just joined after a problem exited. We update the list of the
	 * clients.
	 * 
	 * @throws Exception
	 */
	@Override
	public void clientJoined(Map<String, String> newOtherClients) throws Exception {
		this.otherClients = newOtherClients;
		System.out.println(
				clientName + ": updated client list, array is: " + Arrays.toString(otherClients.entrySet().toArray()));
	}

	/**
	 * All the clients have a variable to keep track about the next number to be
	 * tested. Then, the n different clients will have n different values for the
	 * number to be tested next. The goal is to look at all these numbers and test
	 * the biggest one, since then no client has already tested it.
	 * 
	 * @return
	 * @throws Exception
	 */
	@Override
	public int getNextNumberToTest() throws Exception {
		return this.nextNumberToTest;
	}

	@Override
	public void makeClientsExit() throws Exception {
		System.out.println("Server told it's over");

		isSolutionFound = true;

        this.nextNumberToTest = 0;
        cCommHandler.currProblem = null;
        cCommHandler.currProblemSize = 0;

		// Time so that the HackingThread has the time needed to stop before being notified to start
        // hacking again. If removed, immediately after this method it is called via RMI the startHackingRemote
        // method which makes the opposite stuff. It created another thread and also makes the non stopped
        // thread from previous computation restart computing
		Thread.sleep(50);

        //cCommHandler.currProblemSize = -1;
        //cCommHandler.currProblem = null;
	}

	public boolean isSolutionFound() {
		return isSolutionFound;
	}

	public static void main(String[] args) throws Exception {
		System.setProperty("java.security.policy", "file:./security.policy");
		System.out.println("ip = " + String.valueOf(InetAddress.getLocalHost()).split("/")[1]);

		// If the client is the first client it alerts the other clients
		if (args.length == 2) {
			new Client(args[0], Integer.parseInt(args[1])).startHack();
			//new Client(args[0], Integer.parseInt(args[1]));
		}

		// If the client is not the first client it waits for a first's message
		// serverAddress clientName clientPort
		else {
			//new Client(args[0], args[1], Integer.parseInt(args[2])).startHackingRemote();
			new Client(args[0], args[1], Integer.parseInt(args[2]));
		}

	}

}
