package com.companyDaniVini;/*
  Names: Lee Page, Edward Banner
  Class: CIS 410 - Networks
  Assignment: 5
  Due date: April 18 2012
*/

import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

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
    public int slowStart;
    public int ackTriplicado;
    public int ultimoBlocoEnviado;
    public List<byte[]> arquivo = new ArrayList<>();

    public SenderTFTP(InetAddress address, int port, DataInputStream fileData, DatagramSocket senderSocket) {
        this.senderSocket = senderSocket;
        this.fileData = fileData;
        this.port = port;
        this.address = address;
        this.bay = new byte[526];
        this.ack = 0;
        this.block = 1;
        this.slowStart = 1;
        this.ackTriplicado = 0;
    }

    public void send(int slowStart, int block) {
        this.slowStart = slowStart;
        this.block = block;
        try {

            if(this.block == 1){
                //fazer matriz com bytes
                byte[] arquivoBytes = fileData.readAllBytes();

                int i = 0;
                while(i <= arquivoBytes.length) {
                    byte[] a = new byte[512];
                    a = Arrays.copyOfRange(arquivoBytes, i, Math.min(i + 512, arquivoBytes.length));
                    arquivo.add(montaPacote(a, arquivo.size() + 1));
                    i += 512;
                }
            }

            while(ultimoBlocoEnviado < arquivo.size()){


                List<DatagramPacket> pacotes = new ArrayList<>();
                for(int sl = 0; sl < slowStart; sl ++){
                    if(ultimoBlocoEnviado < arquivo.size()){
                        pacotes.add(new DatagramPacket(arquivo.get(ultimoBlocoEnviado + 1), arquivo.get(ultimoBlocoEnviado + 1).length, address, port));
                        this.ultimoBlocoEnviado ++;
                    } else{
                        break;
                    }

                }
                try {
                    sendWithTimeout(pacotes);
                    slowStart = 2*slowStart;
                } catch (Exception e) {
                    e.printStackTrace();
                }


            }





//            arquivo.forEach(it -> {
//                packet = new DatagramPacket(it, it.length, address, port);
//                try {
//                    sendWithTimeout(packet);
//
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            });

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


    public void sendWithTimeout(List<DatagramPacket> packets) throws Exception {

        try {
            senderSocket.setSoTimeout(500);
        } catch (Exception e) { }

        // set timeout to 500 ms
        boolean retry = true;

        //while(retry) {
            // try to send the packet
            System.out.println("Enviando pacote: " + block);
            packets.forEach(packet -> {
                try {
                    senderSocket.send(packet);
                } catch (IOException e) {
                    System.out.println("Timeout");
                    send(1, block);
                }
            });


            // wait around to receive a packet -- timeout could occur
            //setando 8 bytes para o ack -- se quiser arquivo maior tem que aumentar aqui
            AtomicBoolean r = new AtomicBoolean(false);
            packets.forEach(packet -> {
                byte[] ackBytes = new byte[8];
                //ackBytes = Integer.toString(ack).getBytes();
                ackPacket = new DatagramPacket(ackBytes, ackBytes.length);
                try {
                    senderSocket.receive(ackPacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }


                String ackPacketData = new String(ackPacket.getData()).trim();
                int ackRecebido = Integer.parseInt(ackPacketData);

                System.out.println("Recebeu ack " + ackRecebido);
                if(ackRecebido - 1 == block){
                    block ++;
                    this.ack = ackRecebido;
                } else {
                    //se o ack recebido eh o mesmo que foi recebido da ultima vez aumenta o contador de repeticao
                    if(ackRecebido == ack){
                        ackTriplicado ++;
                    } else {
                        //se o ack recebido eh diferente do que foi recebido na ultima vez, seta o ultimo ack e limpa o contador de repeticoes
                        this.ack = ackRecebido;
                        ackTriplicado = 0;
                    }
                }

                if(ackTriplicado == 3){
                    r.set(true);
                    packet.setData(arquivo.get(ack));
                }
            });
            if(ack <= ultimoBlocoEnviado);
            send(1, block);
         //   retry = r.get();
       // }
    }

    public void sendWithTimeout(DatagramPacket packet) throws Exception {

        try {
            senderSocket.setSoTimeout(500);
        } catch (Exception e) { }

        // set timeout to 500 ms
        boolean retry = true;

        while(retry) {
            try {
                // try to send the packet
                System.out.println("Enviando pacote: " + block);
                senderSocket.send(packet);

                // wait around to receive a packet -- timeout could occur
                //setando 8 bytes para o ack -- se quiser arquivo maior tem que aumentar aqui
                byte[] ackBytes = new byte[8];
                //ackBytes = Integer.toString(ack).getBytes();
                ackPacket = new DatagramPacket(ackBytes, ackBytes.length);
                senderSocket.receive(ackPacket);


                String ackPacketData = new String(ackPacket.getData()).trim();
                int ackRecebido = Integer.parseInt(ackPacketData);

                System.out.println("Recebeu ack " + ackRecebido);
                if(ackRecebido - 1 == block){
                    block ++;
                    this.ack = ackRecebido;
                    retry = false;
                } else {
                    //se o ack recebido eh o mesmo que foi recebido da ultima vez aumenta o contador de repeticao
                    if(ackRecebido == ack){
                        ackTriplicado ++;
                    } else {
                        //se o ack recebido eh diferente do que foi recebido na ultima vez, seta o ultimo ack e limpa o contador de repeticoes
                        this.ack = ackRecebido;
                        ackTriplicado = 0;
                    }
                    retry = false;
                }

                if(ackTriplicado == 3){
                    retry = true;
                    packet.setData(arquivo.get(ack));
                }


            } catch (SocketTimeoutException e) {
                System.err.println("Timeout, vai enviar o bloc: " + block);
                retry = true;
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
