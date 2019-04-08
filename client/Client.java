package assignment3.client;

import assignment3.server.ServerCommInterface;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.security.MessageDigest;

/**
 * Client implementation to be run on Raspberry PIs. Clients belong to a team
 * which communicates with the server. Only one client (master) is directly
 * connected to the server at a time; clients are all connected among each other
 * and communicate with each other. The master client receives problems from the
 * server in the form of an MD5 hash and an integer problem size as an upper
 * bound. The task is to crack the hash, which is originally a numerical string
 * between 0 and, the problem size by using a brute force approach (simply
 * hashing every numerical string in the given range and testing for
 * correspondance with the given problem hash). The clients have to cooperate by
 * working in parallel to solve the problem and submit a solution to the server,
 * and also compete against other teams.
 *
 * @author Emanuela Giovanna Calabi - 13186
 * @author Davide Pizzirani - 13413
 * @author Ravinder Singh - 13642
 */
// Compiled by IntelliJ IDEA to default folder; start clients with the following commands:
// e.g. with 3 clients called client1, client2, client3 communicating on ports 2001, 2002, 2003 respectively
//		java -cp out/production/DS2018 assignment3.client.Client client1 2001
//	    java -cp out/production/DS2018 assignment3.client.Client 192.168.30:2001/client1 client2 2002
//      java -cp out/production/DS2018 assignment3.client.Client  192.168.30:2001/client1 client3 2003
public class Client extends UnicastRemoteObject implements ClientsSync {
	private static final long serialVersionUID = 8341296131121191226L;

	/**
	 * Defines the possible types of events in the log
	 */
	public enum Action {
		JOIN, REGISTER, PROBLEM, START, DONE, SOLUTION
	}

	public final String SERVER_NAME = "rmi://192.168.1.100/server";
	public final String TEAM_NAME = "CryptoKitties";
	// random number to identify the team
	public final String TEAM_CODE = "506840";

	// for receiving problems from the server wia RMI
	public ClientCommHandler clientComm;
	// for registration and solution submission to the server via RMI
	public ServerCommInterface serverComm;
	// this does the cracking
	public WorkerThread worker;

	public String clientName;
	public int clientPort;
	public String clientAddress;

	// list of clients (names with corresponding addresses)
	public Map<String, String> clients;
	// each client keeps record of the events in the log
	public ArrayList<LogEntry> log;


	/**
	 * Consctructor for when the client is the first to connect. It registers
	 * the team to the server as master.
	 *
	 * @param clientName The name of this client to enable RMI
	 * @throws Exception
	 */
	public Client(String clientName, int clientPort) throws Exception {
		setup(clientName, clientPort);
		clients = new HashMap<>();
		clients.put(this.clientName, this.clientAddress);

		// Register timeout the server
		serverComm.register(TEAM_NAME, TEAM_CODE, clientComm);
		broadcast(Action.JOIN, -1, null);
		broadcast(Action.REGISTER, -1, null);
	}

	/**
	 * Constructor for when a slave client is created. It connects with the
	 * master and updates the other clients that it has joined the team.
	 *
	 * @param masterAddress Address of the first connected client to pass the address of this
	 *                      client
	 * @param clientName    The name of the client just connected
	 * @throws RemoteException
	 * @throws AlreadyBoundException
	 */
	public Client(String masterAddress, String clientName, int clientPort) throws Exception {
		setup(clientName, clientPort);
		// rmi to get a list of clients in the team from the master and then add
		// itself to the list
		ClientsSync rmi = (ClientsSync) Naming.lookup("rmi://" + masterAddress);
		clients = rmi.getClients();
		clients.put(this.clientName, this.clientAddress);

		// start the working thread immediately if there is a problem
		LogEntry problem = rmi.getLastProblem();
		if (problem != null) {
			log.add(problem);
			System.out.println(problem);
			worker.problemIndex = 0;
			worker.problemSize = problem.size;
			worker.problemHash = problem.hash;
			worker.doWork = true;
		} else{
			System.out.println("Problem is empty");
		}
		// Communicate to all members that client joined
		broadcast(Action.JOIN, -1, null);
	}

	/**
	 * Setup client for RMI and get the worker thread ready for cracking hashes.
	 */
	private void setup(String clientName, int clientPort) throws Exception {
		this.clientName = clientName;
		this.clientPort = clientPort;
		this.clientAddress = getAddressPort(); //UnknownHostException
		this.worker = new WorkerThread();

		clientComm = new ClientCommHandler(this); //RemoteException
		serverComm = (ServerCommInterface) Naming.lookup(SERVER_NAME); //NotBoundException, MalformedURLException, RemoteException
		log = new ArrayList<>();

		LocateRegistry.createRegistry(clientPort); //RemoteException
		Naming.bind("rmi://" + getAddressPort() + "/" + clientName, this); //AlreadyBoundException, MalformedURLException, RemoteException
	}

	/**
	 * Quickly reference latest problem; returns the index within the log where
	 * the problem is stored
	 */
	public LogEntry getLastProblem() {
		if (worker.problemIndex < 0)
			return null;
		synchronized (log) {
			return log.get(worker.problemIndex);
		}
	}

	/**
	 * Get information about the client.
	 */
	private String getAddressPort() {
		try {
			return InetAddress.getLocalHost().getHostAddress() + ":" + clientPort;
		} catch (UnknownHostException e) {
			System.out.println("ERROR: unknown host");
		}
		return null;
	}

	/**
	 * Adds message to the log array list. Then it notifies the message to the
	 * other clients.
	 *
	 * @param action
	 * @param size
	 * @param hash
	 * @throws Exception
	 */
	public void broadcast(Action action, int size, byte[] hash) {
		// create log entry depending on the contents
		LogEntry message;
		if (action == Action.PROBLEM)
			message = new LogEntry("", SERVER_NAME, action, size, hash);
		else message = new LogEntry(clientName, getAddressPort(), action, size, hash);

		// write message to own log and take action accordingly
		writeLog(message);
		// communicate message to all other clients
		ClientsSync rmi;
		for (Map.Entry<String, String> c : clients.entrySet()) {
			if (!c.getKey().equals(clientName) && !c.getValue().equals(clientAddress)) {
				try {
					rmi = (ClientsSync) Naming.lookup("rmi://" + c.getValue() + "/" + c.getKey());
					// write message to other clients' logs and take action accordingly
					rmi.writeLog(message);
				} catch (Exception e) {
					System.out.println("ERROR: problem contacting client " + c.getValue() + "/" + c.getKey());
					// remove client from the list in case unreachable
					clients.remove(c.getKey());
				}
			}
		}
	}

	/**
	 * DO NOT USE THIS: ONLY TO USE INTERNALLY (USE BROADCAST INSTEAD)
	 * Add message to log and take action accordingly
	 */
	public void writeLog(LogEntry message) {
		synchronized (log) {
			// following line for debugging purposes only
			System.out.printf("[%d] %s\n", log.size(), message);
			log.add(message);
		}

		// react to message
		switch (message.action) {
			// client joined: add new client to list
			case JOIN:
				clients.put(message.authorName, message.authorAddress);
				break;

			// server published problem: start worker thread to crack hash
			case PROBLEM:
				synchronized (worker) {
					worker.problemHash = message.hash;
					worker.problemSize = message.size;
					worker.problemIndex = log.size() - 1;
					worker.doneAll = false;
				}
				break;

			// client finds solution: stop worker thread and reset
			case SOLUTION:
				synchronized (worker) {
					worker.problemSize = -1;
					worker.doWork = false;
					worker.doneAll = false;
				}
				break;

			// client started cracking subrange: start worker thread or keep it active
			case START:
				synchronized (worker) {
					worker.doWork = true;
				}
			default:
				break;
		}
	}

	/**
	 * Get list of clients' names and addresses
	 */
	public Map<String, String> getClients() {
		return clients;
	}


	/**
	 * Does all the cracking, communication is handled by the client enclosing it
	 * Problem size is split into smaller subranges which can be worked on in
	 * parallel by the different clients.
	 */
	public class WorkerThread extends Thread {
		public byte[] problemHash;
		public int problemSize;
		// problem size is split into smaller subranges the single clients can
		// work on in parallel
		public int range = 10000;
		// index within the log of the current/latest problem
		public int problemIndex;
		// used to start/stop the worker thread
		public boolean doneAll, doWork;

		public WorkerThread() {
			problemIndex = -1;
			problemHash = null;
			problemSize = 0;
			doneAll = false;
			doWork = false;
		}

		public void run() {
			try {
				MessageDigest md = MessageDigest.getInstance("MD5"); //NoSuchAlgorithmException
				problemIndex = -1;

				// keep the working thread alive
				while (true) {
					// work only when there's actually work to do
					while (problemSize > 0 && doWork) {
						// uncomment for debugging purposes only
						//System.out.println("problem index: " + problemIndex);

						int from, solution;
						// get the first subrange that has not been cracked
						from = getAvailableRange(problemIndex);
						// inform other clients about the subrange which is currently been worked on
						broadcast(Action.START, from, null);
						Thread.sleep(5); // added for synchronisation
						solution = crackRange(md, from, range);
						// inform other client subrange has been processed and need not be worked on further
						broadcast(Action.DONE, from, null);

						// check if the solution was actually found
						if (solution != -1 && solution <= problemSize) {
							broadcast(Action.SOLUTION, solution, problemHash); // broadcast first so other clients stop
							broadcast(Action.REGISTER, -1, null);
							// the client to find the solution becomes the new master and submits to the server
							serverComm.reregister(TEAM_NAME, TEAM_CODE, clientComm);
							serverComm.submitSolution(TEAM_NAME, TEAM_CODE, String.valueOf(solution));
							// reset to idle and break
							problemHash = null;
							problemSize = 0;
							break;
						}
					}

					Thread.sleep(5); // wait for problem
				}
			} catch (Exception e) {
				this.run(); // to avoid being interrupted
			}
		}

		/**
		 * Get a subrange which has not been worked on by the clients yet.
		 */
		private int getAvailableRange(int index) {
			LogEntry current; // pivot

			// if all subranges have not yet been worked on, find the missing one(s)
			if (!doneAll) {
				int start;

				synchronized (log) {
					if (index == log.size() - 1)
						return 0; // start from 0 if no work was done
					// read log from the last entry to find the subrange that was
					// worked on most recently, use the next available subrange
					for (int i = log.size() - 1; i > index; i--) {
						current = log.get(i);
						if (current.action == Action.START) {
							start = current.size + range;
							// all subranges have been worked on
							if (start >= problemSize)
								doneAll = true;
							return start;
						}
					}
				}
			}
			// if all subranges have been worked on, find the ones which have not
			// yet been completed (e.g. due to death of a client, etc.)
			else {
				// build a list of the subranges that have been completed
				ArrayList<Integer> done = new ArrayList<>();
				synchronized (log) {
					for (int i = index; i < log.size() - 1; i++) {
						current = log.get(i);
						if (current.action == Action.DONE) {
							done.add(current.size);
						}
					}
				}

				// sort the list and return the first gap
				Collections.sort(done);
				int temp = 0;
				for (int i = 0; i < done.size(); i++) {
					temp += range;
					if (temp != done.get(i)) {
						return temp;
					}
				}
			}
			// no available subranges have been found
			return -1;
		}

		/**
		 * Brute force hash each number in the subrange, then compare it to the
		 * problem given by the server. Return the current number if it corresponds
		 * to the given hash, otherwise keep on looping. Return -1 if the current
		 * subrange does not contain the solution.
		 */
		public int crackRange(MessageDigest md, int from, int range) {
			int to = from + range;
			if (to > problemSize) to = problemSize;
			int i = from;

			do {
				if (Arrays.equals(problemHash, md.digest((String.valueOf(i)).getBytes())))
					return i;
				i++;
			} while (i < to);

			return -1;
		}

	}

	// start application, use command line arguments
	public static void main(String[] args) throws Exception {
		System.setProperty("java.security.policy", "file:./security.policy");
		System.setProperty("java.rmi.server.hostname", InetAddress.getLocalHost().getHostAddress()); //UnknownHostException
		System.setSecurityManager(new SecurityManager());

		Client client;
		// If the client is the first client it alerts the other clients
		if (args.length == 2)
			// clientName, clientPort
			client = new Client(args[0], Integer.parseInt(args[1]));

		// If the client is not the first client it waits for a first's message
		// masterAddress clientName clientPort
		else
			// clientName, masterAddressAndPort, clientPort
			client = new Client(args[0], args[1], Integer.parseInt(args[2]));
		client.worker.start(); // start worker
	}
}
