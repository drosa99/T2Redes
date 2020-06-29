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
		final String dir =  "/Users/daniela/Desktop/Redes/T2Redes-Daniela-ViniciusLima/src/com/companyDaniVini";
		System.out.println("current dir = " + dir);


		// initialize some variables
		int block = 0;
		DataOutputStream fileOut = null;
		DataInputStream fileData = null;
		byte[] bay = new byte[526];
		String server = "localhost";
		String request = "read";
		String fileName = "/Users/daniela/Desktop/Redes/T2Redes-Daniela-ViniciusLima/src/com/companyDaniVini/server/test.jpg";
		String name = "test.jpg";

		try {
			// prepare the RRQ data to send to server
			// populates bay with [01][fileName][0][octet][0]
			ClientTFTP.prepareDataRRQ(bay, fileName, "octet");

			// first block receiver plans to receive is 1
			block = 1;
			// create generic socket
			DatagramSocket cliSkt = new DatagramSocket(); // no specific
			// port

			InetAddress ipAddr = InetAddress.getByName(server);
			int port = DEFAULT_PORT;
			DatagramPacket pkt = new DatagramPacket(bay, bay.length, ipAddr, port);


			cliSkt.setSoTimeout(500);
			cliSkt.send(pkt);

			File file = new File("./destino/" + name);
			fileOut = new DataOutputStream(new FileOutputStream(file));


			cliSkt.receive(pkt);


			port = pkt.getPort();
			ipAddr = pkt.getAddress();
			int blockNum = ReceiverUtils.handlePacket(cliSkt, pkt, fileOut, ipAddr, port, 0, pkt.getLength());
		//	bay = pkt.getData();



			// port for server has changed -- get the new port


			// open up a stream for writing



//			// write the first packet into file
//			if (pkt.getLength() > 150) {
//				//fileOut.write(bay, 4, pkt.getLength() - 4);
//				System.out.println("entrou");
//				String mensagem = new String(bay);
//				if(mensagem.split("Dados:").length < 2){
//					System.out.println("mensagem" + mensagem);
//				}
//				mensagem = mensagem.split("Dados:")[1];
//				byte[] mensagemBytes = mensagem.getBytes();
//
//				fileOut.write(mensagemBytes, 0, mensagemBytes.length);
//			}
//
//
//			// create first ACK with block number 1
//			byte[] ackbuf = Integer.toString(1).getBytes();
//			DatagramPacket ackPkt = new DatagramPacket(ackbuf, ackbuf.length, ipAddr, port);
//			// send the first ACK
//			UtilTFTP.puts("received packet: " + 1 + " size: " + pkt.getLength());
//			String ack = new String(ackbuf);
//			System.out.println("Solicita o ACK " + ack);
//			cliSkt.send(ackPkt);

			//use receiver instead.
			if (pkt.getLength() >= 512) {
				ReceiverTFTP receiver = new ReceiverTFTP(ipAddr, port, fileOut, cliSkt, blockNum);
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
