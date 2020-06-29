package com.companyDaniVini;/*
  Names: Lee Page, Edward Banner
  Class: CIS 410 - Networks
  Assignment: 5
  Due date: April 18 2012
*/

import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class SenderTFTP {

    public DatagramSocket senderSocket;
    public DatagramPacket packet;
    public DatagramPacket ackPacket;
    public DataInputStream fileData;
    public InetAddress address;
    public int port;
    public byte[] bay;
    public int ack;
    public int block;
    public int bytesRead;
    public long crc;

    public SenderTFTP(InetAddress address, int port, DataInputStream fileData, DatagramSocket senderSocket) {
        this.senderSocket = senderSocket;
        this.fileData = fileData;
        this.port = port;
        this.address = address;
        this.bay = new byte[526];
        this.ack = 0;
        this.block = 1;
        this.crc = 0L;
    }

    public void send() {

        try {
            //fazer matriz com bytes
            List<byte[]> arquivo = new ArrayList<>();
            byte[] arquivoBytes = fileData.readAllBytes();

            int i = 0;
            while(i <= arquivoBytes.length) {
                byte[] a = new byte[512];
                a = Arrays.copyOfRange(arquivoBytes, i, Math.min(i + 512, arquivoBytes.length));
                arquivo.add(montaPacote(a, arquivo.size() + 1));
                i += 512;
            }



            arquivo.forEach(it ->{
                packet = new DatagramPacket(it, it.length, address, port);
                try {
                    sendWithTimeout(packet);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

        } catch(Exception e) {
            System.out.println(e);
        }
    }

    private byte[] montaPacote(byte[] segmento, int nrBloco) {
        byte[] bay = new byte[526];
        insertBlockNumber(bay, nrBloco);
        bay = insertCRC(bay, segmento);
        System.out.println("bay " + new String(bay));
        return bay;
    }

    public void sendWithTimeout(DatagramPacket packet) throws Exception {

        // make packet for acks
//        byte[] ackBytes = new byte[2];
//        ackBytes = Integer.toString(ack).getBytes();
//        ackPacket = new DatagramPacket(ackBytes, ackBytes.length);

        try {
            senderSocket.setSoTimeout(500);
        } catch (Exception e) { }

        // set timeout to 500 ms
        int retry = 0;
        int ackRetry = 0;

        while(retry < 3) {
            try {
                // try to send the packet
                senderSocket.send(packet);

                // wait around to receive a packet -- timeout could occur
                //setando 8 bytes para o ack -- se quiser arquivo maior tem que aumentar aqui
                byte[] ackBytes = new byte[8];
                //ackBytes = Integer.toString(ack).getBytes();
                ackPacket = new DatagramPacket(ackBytes, ackBytes.length);
                senderSocket.receive(ackPacket);


                if (ackPacket.getPort() != port) {  // packet came from foreign port
                    // send an error packet to the foreign source
                    byte[] errorData = PacketTFTP.makeErrorData(5, "Unknown transfer ID.");
                    DatagramPacket errorPacket = new DatagramPacket(errorData, errorData.length, ackPacket.getAddress(), ackPacket.getPort());
                    senderSocket.send(errorPacket);

                    // disregard packet and resend previous
                    continue;
                }

                //TODO TIRAR ACK DAQUI E COLOCAR PRA FORA
                // got ACK, check to make sure block number is correct
                String ackPacketData = new String(ackPacket.getData()).trim();
                ack = Integer.parseInt(ackPacketData);

                System.out.println("Recebeu ack " + ack);

                //TODO aqui manda novamente o pacote que ele ta pedindo
                if (ackRetry == 3) {
                    System.out.println("3 INCORRECT ACKS -- EXITING");
                    System.exit(1);
                }

                // made it all the way through without timing out and correct
                // block #
                break;

            } catch (SocketTimeoutException e) {
                System.out.println("TIMEOUT FROM SENDER");
                System.out.println(e);
                retry++;
                break;
            }
        }
    }

    public void insertBlockNumber(byte[] bay, int bloco) {
        // populate data packet byte array with opcode of DATA
        bay[0] = 0;
        bay[1] = 3;
        bay[2] = (byte)(bloco >>> 8);
        bay[3] = (byte)(bloco & (int)0xff);
    }

    public byte[] insertCRC(byte[] bay, byte[] conteudo){
        //monta o pacote com todos os dados que deve ter
        ByteBuffer buffer = ByteBuffer.allocate(526);
        buffer.put(bay[0]);
        buffer.put(bay[1]);
        buffer.put(bay[2]);
        buffer.put(bay[3]);
        byte[] crcbytes = new byte[10];
        long crc = UtilTFTP.getCRC(conteudo);
        //aqui forca o crc a ter 10 bytes
        crcbytes = Arrays.copyOf(Long.toString(crc).getBytes(), 10);
        buffer.put(crcbytes);
        buffer.put(conteudo);
        return buffer.array();

    }
}
