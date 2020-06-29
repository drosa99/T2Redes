package com.companyDaniVini;

import java.net.*;
import java.nio.file.*;
import java.io.*;
import java.util.*;

public class Client {

	final static int DEFAULT_PORT = 6969;

	public static void main(String[] args) {
		// error checking
		String usage = "Guide: java client server [read or write] file mode ";


		//System.getProperty("user.dir") +
		final String dir =  System.getProperty("user.dir") + "/src/com/companyDaniVini";
		System.out.println("current dir = " + dir);


		// initialize some variables
		int block = 0;
		DataOutputStream fileOut = null;
		DataInputStream fileData = null;
		byte[] bay = new byte[526];
		String server = "localhost";
		String request = "read";
		String fileName = System.getProperty("user.dir") + "/src/com/companyDaniVini/server/test.jpg";
		String name = "test.jpg";

		try {
			// prepare the RRQ data to send to server
			// populates bay with [01][fileName][0][octet][0]
			Client.prepareDataRRQ(bay, fileName, "octet");

			// first block receiver plans to receive is 1
			block = 1;
			// create generic socket
			DatagramSocket cliSkt = new DatagramSocket(); // no specific
			// port

			InetAddress ipAddr = InetAddress.getByName(server);
			int port = DEFAULT_PORT;
			DatagramPacket pkt = new DatagramPacket(bay, bay.length, ipAddr, port);


			//cliSkt.setSoTimeout(500);
			cliSkt.send(pkt);

			File file = new File("./destino/" + name);
			fileOut = new DataOutputStream(new FileOutputStream(file));

			DatagramPacket tampkt = new DatagramPacket(new byte[8], 8, ipAddr, port);
			cliSkt.receive(tampkt);

			port = tampkt.getPort();
			ipAddr = tampkt.getAddress();

			int qntdPacotes = Integer.parseInt(new String(tampkt.getData()).trim());
			System.out.println("Recebeu a quantidade de pacotes do arquivo: " + qntdPacotes);


			DatagramPacket ackPkt = new DatagramPacket(tampkt.getData(), tampkt.getData().length, ipAddr, port);
			System.out.println("Vai mandar ACK da quantidade de pacotes do arquivo");
			cliSkt.send(ackPkt);

			bay = new byte[526];
			pkt = new DatagramPacket(bay, bay.length, ipAddr, port);
			int blockNum = ReceiverUtils.handlePacket(cliSkt, pkt, fileOut, ipAddr, port, 0, pkt.getLength());

			//use receiver instead.
			if (pkt.getLength() >= 512) {
				ReceiverTFTP receiver = new ReceiverTFTP(ipAddr, port, fileOut, cliSkt, blockNum, qntdPacotes);
				receiver.receive();
			}else{
				fileOut.close();
				UtilTFTP.puts("transfer ends here");
			}


		} catch (Exception e) {
			System.out.println(e);
		}


	}

	public static void prepareDataRRQ(byte[] bay, String fileName, String mode) {
		int index;
		// set first two bytes of outgoing packet to 01 to indicicate RRQ
		bay[0] = 0;
		bay[1] = 1;

		/*
		 * populate next free bytes in bay with the ``fileName" followed by null
		 * character
		 */
		for (index = 0; index < fileName.length(); index++) {
			bay[index + 2] = (byte) fileName.charAt(index);
		}
		index += 2;
		bay[index++] = 0; // bay is now [02][filename][0]

		/*
		 * populate next free bytes in bay with ``octet" followed by null
		 * character
		 */
		for (int i = 0; i < mode.length(); i++) {
			bay[index++] = (byte) mode.charAt(i);
		}
		bay[index] = 0; // bay is now [02][filename][0][octet][0]
	}

	public static void prepareDataWRQ(byte[] bay, String fileName, String mode) {
		int index;
		// set first two bytes of outgoing packet to 02 to indicicate WRQ
		bay[0] = 0;
		bay[1] = 2;

		/*
		 * populate next free bytes in bay with the ``fileName" followed by null
		 * character
		 */
		for (index = 0; index < fileName.length(); index++) {
			bay[index + 2] = (byte) fileName.charAt(index);
		}
		index += 2;
		bay[index++] = 0; // bay is now [02][filename][0]

		/*
		 * populate next free bytes in bay with ``octet" followed by null
		 * character
		 */
		for (int i = 0; i < mode.length(); i++) {
			bay[index++] = (byte) mode.charAt(i);
		}
		bay[index] = 0; // bay is now [02][filename][0][octet][0]
	}

	public static boolean incorrectBlockRRQ(byte[] bay, int block) {
		return bay[0] != 0 || bay[1] != 3 || bay[2] != 0 || (int) bay[3] != block;
	}

	public static boolean incorrectBlockWRQ(byte[] bay, int block) {
		return bay[0] != 0 || bay[1] != 4 || bay[2] != 0 || (int) bay[3] != block;
	}

	private static boolean sendWithTimeout(DatagramSocket cli, DatagramPacket pkt, String request, int block)
			throws Exception {
		int retry = 0;
		byte[] bay = new byte[516];

		while (retry < 3) {
			try {
				System.out.println("retry");
				cli.setSoTimeout(500);
				cli.send(pkt);
				// pkt = new DatagramPacket(bay, bay.length);
				cli.receive(pkt);
				bay = pkt.getData();

				System.out.println("bay 179: " + new String(bay));


				break;
			} catch (SocketTimeoutException e) {
				System.out.println("time out");
				retry++;
				continue;
			}
		}
			return true;
	}

	private static void disconnect(){
		UtilTFTP.puts("shutdown");
		System.exit(1);
	}
}
