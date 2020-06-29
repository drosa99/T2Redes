package com.companyDaniVini;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Arrays;

public class ReceiverTFTP {

	public DatagramSocket skt;
	public DatagramPacket pkt;
	public DatagramPacket ackPkt;
	public DataOutputStream fileOutput;
	public InetAddress ipAddr;
	public int port;
	public byte[] bay;
	public int ack;
	public int blockNum;
	public int packetSize;


	public ReceiverTFTP(InetAddress ipAddr, int port, DataOutputStream fileOutput, DatagramSocket skt, int block) {
		this.skt = skt;
		this.fileOutput = fileOutput;
		this.port = port;
		this.ipAddr = ipAddr;
		this.ack = 2;
		this.bay = new byte[526];
		this.ackPkt = new DatagramPacket(Integer.toString(ack).getBytes(), Integer.toString(ack).getBytes().length, ipAddr, port);
		this.blockNum = block;
		this.pkt = new DatagramPacket(bay, bay.length);
	}


	// wait for a packet to come and write the contents to outputStream
	public void receive() {

		try {
			while (true) {
				skt.setSoTimeout(500);
				try{
				skt.receive(pkt);
				bay = pkt.getData();
				this.blockNum = ReceiverUtils.handlePacket(skt, pkt, fileOutput, ipAddr, port, blockNum, packetSize);
				} catch (SocketTimeoutException ex) {
					break;

				}
			}   // quit once a packet of has less than 512 bytes of data

			fileOutput.close();
			System.out.println("Transfencia concluida");
		} catch (Exception e) {
			System.out.println(e);
		}
	}

}

class ReceiverUtils{

	public static int handlePacket(DatagramSocket skt, DatagramPacket pkt, DataOutputStream fileOutput, InetAddress ipAddr,
																	int port, int blockNum, int packetSize) throws IOException {
		//escreve os dados do pacote no arquivo se tiver certo
		byte[] bay = pkt.getData();
		System.out.println("Ultimo bloco processado: " + blockNum);
		if (checkRightPacket(bay, blockNum)) {
			// write next 512 bytes to file
			byte[] mensagemBytes = Arrays.copyOfRange(bay, 14, bay.length);
			fileOutput.write(mensagemBytes, 0, mensagemBytes.length);
			// increment to the next block
			blockNum++;
			System.out.println("Escreveu bloco: " + blockNum);
		}

		// get packet size to make sure it's less that 512 bytes
		packetSize = pkt.getLength();

		// mandando ack
		int ack = blockNum + 1;
		System.out.println("Vai mandar o ack " + ack);
		byte[] ackNext = Integer.toString(ack).getBytes();
		DatagramPacket ackPkt = new DatagramPacket(ackNext, ackNext.length, ipAddr, port);
		skt.send(ackPkt);
		return blockNum;
	}

	private static boolean checkRightPacket(byte[] bay, int blockNum) {
		//compara numero do bloco recebido com o numero do bloco que espera receber
		int sentBlockNumber = PacketTFTP.getBlockNumber(bay);
		System.out.println("Recebeu o pacote: " + sentBlockNumber);

		if (sentBlockNumber != blockNum + 1) {
			return false;
		}

		//compara crc32
		return UtilTFTP.checkCRC(bay);
	}

}