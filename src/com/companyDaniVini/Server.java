package com.companyDaniVini;

import java.net.*;
import java.io.*;
import java.util.*;

public class Server extends Thread {

	DatagramPacket pkt;
	DatagramPacket ackPkt;

	final static int DEFAULT_PORT = 6969;

	public Server (DatagramPacket pkt) {
		this.pkt = pkt;
	}

	public void run() {
		byte[] buf = new byte[516];
		// get sending information of client
		int port = pkt.getPort();
		InetAddress address = pkt.getAddress();

		try {
			// open up a new socket for data flow
			DatagramSocket serverSocket = new DatagramSocket();

			//add shutdown hook
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					serverSocket.close();
					System.out.println("Server Socket closed");
				}
			});
			// assume the datagram buffer contains characters
			buf = pkt.getData();

			// get the file name from the packet
			String fileName = getFileName(buf);

			// get mode from the packet
			String mode = getMode(buf);

			// if the mode is something other than ``octet" then quit
			if (!mode.equals("octet")) {
				byte[] errorData = PacketTFTP.makeErrorData(0, "Mode other than octet.");
				DatagramPacket errorPacket = new DatagramPacket(errorData, errorData.length, address, port);
				serverSocket.send(errorPacket);
				System.exit(1);
			}


				try {
					// check to see if file exists
					if (!new File(fileName).exists()) {
						byte[] errorData = PacketTFTP.makeErrorData(1, "File not found.");
						DatagramPacket errorPacket = new DatagramPacket(errorData, errorData.length, address, port);
						serverSocket.send(errorPacket);
						System.exit(1);
					}

					// open up a reader on the file
					DataInputStream fileData = new DataInputStream(new FileInputStream(fileName));

					// create a new SenderTFTP object and start the sending process
					SenderTFTP sender = new SenderTFTP(address, port, fileData, serverSocket);
					sender.send();

				} catch (Exception e) {
					System.out.println(e);
				}
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	// make sure the request packet is speaking in terms of octet
	// get the index number after the end of fileName
	public String getMode(byte[] b) {
		int index = 2;  // start looking right after [01]
		while (b[index++] != 0);

		// get the length of whatever is sanwiched between the two null
		// byte [01fileName0*we_want_this_length*0]
		int length = 0;
		int offset = index;
		while (b[index++] != 0)
			length++;

		// create a new string the will contain the mode
		return new String(b, offset, length);
	}

	// extract the file name out of request packet
	public String getFileName(byte[] b) {
		String fileName = "";
		int i = 2;
		while (b[i] != 0) {
			fileName += (char)b[i++];
		}
		return fileName;
	}

	public static void main(String [] args) {

		try {


			int currentPort = 0;

			if(args.length == 1){
				currentPort =  Integer.parseInt(args[0]);
			}else{
				currentPort = DEFAULT_PORT;
			}
			UtilTFTP.puts("Open Socket at: "+ currentPort);
			DatagramSocket srv = new DatagramSocket(currentPort);

			while (true) {
				byte [] buf = new byte[516];
				DatagramPacket pkt = new DatagramPacket(buf, buf.length);
				srv.receive(pkt);
				buf = pkt.getData();
				System.out.println("buf " + new String(buf));
				// create a new thread to handle request and continue listening
				new Server(pkt).run();
			}
		} catch (Exception e) {
			System.err.println(e);
		}
	}
}
