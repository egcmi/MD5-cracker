package assignment3.client;

import java.io.Serializable;

/**
 * Defines structure of possible entries in the log
 */
public class LogEntry implements Serializable {
	private static final long serialVersionUID = -8923661346264251689L;
	String authorName;
	String authorAddress;
	Client.Action action;
	int size;
	byte[] hash;

	public LogEntry(String authorName, String authorAddress, Client.Action action, int size, byte[] hash) {
		this.authorName = authorName;
		this.authorAddress = authorAddress;
		this.action = action;
		this.size = size;
		this.hash = hash;
	}

	/**
	 * Returns a human readable, printable string representing the log entry object
	 */
	public String toString() {
		return authorAddress + ((authorName != null) ? "/" + authorName : "") + " " + action.name() + " " + ((size > -1) ? size : "") + " "
				+ ((hash != null) ? hashToString() : "");
	}

	/**
	 * Returns a human readable, printable string representing the (problem) hash
	 */
	public String hashToString(){
		StringBuilder sb = new StringBuilder();
		for (byte b : hash) {
			sb.append(String.format("%02x", b & 0xff));
		}
		return sb.toString();
	}
}
