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
    public int ackTriplicado;
    public List<byte[]> arquivo = new ArrayList<>();

    public SenderTFTP(InetAddress address, int port, DataInputStream fileData, DatagramSocket senderSocket) {
        this.senderSocket = senderSocket;
        this.fileData = fileData;
        this.port = port;
        this.address = address;
        this.bay = new byte[526];
        this.ack = 0;
        this.block = 1;
        this.crc = 0L;
        this.ackTriplicado = 0;
    }

    public void send() {

        try {
            //pega o arquivo e coloca ele numa lista de byte[]
            byte[] arquivoBytes = fileData.readAllBytes();

            int i = 0;
            while (i <= arquivoBytes.length) {
                byte[] a = new byte[512];
                a = Arrays.copyOfRange(arquivoBytes, i, Math.min(i + 512, arquivoBytes.length));
                arquivo.add(montaPacote(a, arquivo.size() + 1));
                i += 512;
            }
            System.out.println("\n Arquivo quebrado em:" + arquivo.size() + " pacotes \n");

            sendQntdPacotes();

            //bloco comeca em 1 entao pode ser comparado com quantidade de pacotes
            int slowStart = 0;
            while (ack < arquivo.size()) {

                //ultimo ack recebido é o proximo que tem que ser mandado
                int ultimoAckRecebido = ack;

                //block eh o ultimo bloco enviado
                this.block = Math.max(ack - 1, 1);
                boolean recomeca = false;
                for (int j = 0; j < Math.pow(2, slowStart); j++) {

                    if(ultimoAckRecebido + j >= arquivo.size()) {
                        System.out.println("Transferência encerrada.");
                        System.exit(1);
                    }

                    // pacote mandado tem que ser o ultimo ack recebido
                    System.out.println("\n --- Enviando pacote: " + (ultimoAckRecebido + j) + " ---");
                    packet = new DatagramPacket(arquivo.get(ultimoAckRecebido + j), arquivo.get(ultimoAckRecebido + j).length, address, port);

                    // o metodo sendWithTimeout retorna true se houve timeout ou recebimento de 3 acks duplicados
                    // entao o slow start recomeca a partir do ultimo ack recebido e com expoente 0 para o slow start
                    recomeca = sendWithTimeout(packet);
                    if (recomeca) {
                        slowStart = 0;
                        break;
                    } else {
                        //se nao deve recomecar, incrementa a quantidade de blocos enviados
                        block ++;
                    }
                }
                // se mandou nao recomecar e o ultimo ack recebido == ultimo bloco mandado -> entao aumenta o expoente do slow start
                System.out.println("SLOW START  ack - 1: " + (ack - 1));
                System.out.println("SLOW START block: " + block + "\n");
                if (!recomeca && ack - 1  == block) {
                    slowStart++;
                    System.out.println(" \n - SLOW START  próximo expoente: " + slowStart + "----- \n");
                } else {
                    System.out.println(" \n - SLOW START  recomeçando com expoente 0  ----- \n ");
                    slowStart = 0;
                }
                //System.out.println("SLOW START block: " + block + "\n");

            }

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private byte[] montaPacote(byte[] segmento, int nrBloco) {
        byte[] bay = new byte[526];
        insertBlockNumber(bay, nrBloco);
        bay = insertCRC(bay, segmento);
        return bay;
    }

    public void sendQntdPacotes() {
        try {
            senderSocket.setSoTimeout(500);
        } catch (Exception e) {
        }

        boolean retry = true;
        byte[] qntPacotes = Integer.toString(arquivo.size()).getBytes();
        packet = new DatagramPacket(qntPacotes, qntPacotes.length, address, port);
        while (retry) {
            System.out.println("Enviando quantidade de pacotes: " + arquivo.size());
            try {
                senderSocket.send(packet);

                byte[] ackBytes = new byte[2];
                ackPacket = new DatagramPacket(ackBytes, ackBytes.length);
                senderSocket.receive(ackPacket);

                byte[] ackData = ackPacket.getData();
                System.out.println("Recebeu ack quantidade de pacotes");
                retry = false;
                break;

            } catch (SocketTimeoutException e) {
                System.err.println("Timeout para receber ack da quantidade de pacotes... Mandando novamente...");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean sendWithTimeout(DatagramPacket packet) throws Exception {

        try {
            senderSocket.setSoTimeout(500);
        } catch (Exception e) {
        }

        boolean retry = true;

        try {
            // manda o pacote
            senderSocket.send(packet);

            //setando 8 bytes para o ack -- se quiser arquivo maior tem que aumentar aqui
            byte[] ackBytes = new byte[8];
            ackPacket = new DatagramPacket(ackBytes, ackBytes.length);
            // espera receber o pacote do ack, pode ocorrer timeout
            senderSocket.receive(ackPacket);

            String ackPacketData = new String(ackPacket.getData()).trim();
            int ackRecebido = Integer.parseInt(ackPacketData);

            System.out.println("\n Recebeu ack " + ackRecebido);
//            System.out.println("block " + block);
//            System.out.println("ack " + ack);
//            System.out.println("ackTriplicado " + ackTriplicado);

            //ack confere com o ultimo pacote mandado
            if (ackRecebido - 1 == block) {
                this.ack = ackRecebido;
                retry = false;
                ackTriplicado = 1;

            } else {  //ack nao corresponde ao ultimo pacote enviado
                //se o ack recebido eh o mesmo que foi recebido da ultima vez aumenta o contador de repeticao
                if (ackRecebido == ack) {
                    ackTriplicado++;
                } else if (ack < ackRecebido) {
                    //se o ack recebido eh diferente do que foi recebido na ultima vez, seta o ultimo ack e limpa o contador de repeticoes
                    this.ack = ackRecebido;
                    ackTriplicado = 1;
                }
                retry = false;
            }

            //FAST-RETRANSMIT se recebeu 3x o mesmo ack -> deve recomecar o slow start
            if (ackTriplicado == 3) {
                retry = true;
                System.err.println("Recebeu 3x o ACK: " + ackRecebido);
                packet.setData(arquivo.get(ackRecebido - 1));
            }


        } catch (SocketTimeoutException e) {
            //Houve timeout no recebimento do ack, deve recomecar o slow start
            System.err.println("Timeout, vai recomeçar o slow start");
            retry = true;
        }
        return retry;
    }

    public void insertBlockNumber(byte[] bay, int bloco) {
        // populate data packet byte array with opcode of DATA
        bay[0] = 0;
        bay[1] = 3;
        bay[2] = (byte) (bloco >>> 8);
        bay[3] = (byte) (bloco & (int) 0xff);
    }

    public byte[] insertCRC(byte[] bay, byte[] conteudo) {
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
