package assignment3.client.old;

//import javafx.util.Pair;

import java.rmi.Naming;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;

public class HackingThread extends Thread {

	private Client client;

	public HackingThread(Client client) {
		this.client = client;
	}

    /**
     * Start hacking a number
     */
	@Override
	public void run() {
        System.out.println("Hacking Started...");
		boolean firstFound = false;

		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			Integer toTest = -1;

			System.out.println("Here + " + client.nextNumberToTest + " + " + client.cCommHandler.currProblemSize);

			synchronized (this) {

			    // Continues hacking every number until a solution is not found from the current client ot from another
                // client on the network
				while ((client.nextNumberToTest < client.cCommHandler.currProblemSize) && (!client.isSolutionFound())) {
					// Search the other clients the number to test next
					toTest = client.nextNumberToTest;

					System.out.println(
							"client.nextNumberToTest < client.cCommHandler.currProblemSize && !client.isSolutionFound() = "
									+ (client.nextNumberToTest < client.cCommHandler.currProblemSize
											&& !client.isSolutionFound()));
					System.out.println("isSolutionFound = " + client.isSolutionFound());

					// Check in all the other clients the number to crack next
					for (Map.Entry<String, String> p : client.otherClients.entrySet()) { // TODO: do not search the
																							// actual client
						try {
							ClientsSync rmi = (ClientsSync) Naming.lookup("rmi://" + p.getValue() + "/" + p.getKey());
							int temp = rmi.getNextNumberToTest();
							System.out.println(
									"\t\tSearching client " + p.getValue() + ":" + p.getKey() + ", found " + temp);

							if (temp > toTest)
								toTest = temp;

						} catch (Exception e) {
							// Remove unreachable client from clients list
							client.otherClients.remove(p.getKey());
							System.out.println("Problems contacting client " + p.getValue() + ":" + p.getKey()
									+ ", new list is " + Arrays.toString(client.otherClients.entrySet().toArray()));
						}
					}

					client.nextNumberToTest = toTest + 1;
					System.out.println("Testing number " + toTest);

					// If the number is found we set the isSolutionFound variable to true to make sure that the
                    // while cycle ends
					if (Arrays.equals(client.cCommHandler.currProblem, md.digest(toTest.toString().getBytes()))) {
						System.out.println("Solution found!!!");
						client.isSolutionFound = true;
						firstFound = true;
					}

					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
					}
				}

				// When the thread is over we call the function afterHacked to setup the parameters needed
                // to search for the next number
				System.out.println("Bye from thread...");
                client.afterHacked(firstFound, toTest);

                //System.out.println("----------------------------------------------------------------------");
                return;
			}
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

        //System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		return;
	}

}
