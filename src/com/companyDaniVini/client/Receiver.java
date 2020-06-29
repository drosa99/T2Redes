package com.companyDaniVini.client;

import com.companyDaniVini.Util;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Arrays;

public class Receiver {

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
    public int qntdPacotes;


    public Receiver(InetAddress ipAddr, int port, DataOutputStream fileOutput, DatagramSocket skt, int block, int qntdPacotes) {
        this.skt = skt;
        this.fileOutput = fileOutput;
        this.port = port;
        this.ipAddr = ipAddr;
        this.ack = 2;
        this.bay = new byte[526];
        this.ackPkt = new DatagramPacket(Integer.toString(ack).getBytes(), Integer.toString(ack).getBytes().length, ipAddr, port);
        this.blockNum = block;
        this.pkt = new DatagramPacket(bay, bay.length);
        this.qntdPacotes = qntdPacotes;
    }


    // espera o recebimento dos pacotes
    public void receive() {

        try {
            while (true) {
                skt.setSoTimeout(500);
                try {
                    skt.receive(pkt);
                    bay = pkt.getData();
                    this.blockNum = ReceiverUtils.handlePacket(skt, pkt, fileOutput, ipAddr, port, blockNum, packetSize);
                } catch (SocketTimeoutException ex) {
                    //para a execucao se ja recebeu todos os pacotes
                    if (blockNum == qntdPacotes) break;
                }
            }

            fileOutput.close();
            System.out.println("Transferencia concluida");
        } catch (Exception e) {
            System.out.println(e);
        }
    }

}

class ReceiverUtils {

    //metodo que pega um pacote, verifica se deve ser escrito no arquivo ou nao e manda o ack
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

        packetSize = pkt.getLength();

        // mandando ack
        int ack = blockNum + 1;
        System.out.println("Vai mandar o ack " + ack);
        byte[] ackNext = Integer.toString(ack).getBytes();
        DatagramPacket ackPkt = new DatagramPacket(ackNext, ackNext.length, ipAddr, port);
        skt.send(ackPkt);
        return blockNum;
    }

    //metodo que compara se o bloco deve ser escrito no arquivo ou descartado
    private static boolean checkRightPacket(byte[] bay, int blockNum) {
        //compara numero do bloco recebido com o numero do bloco que espera receber
        int sentBlockNumber = Util.getBlockNumber(bay);
        System.out.println("\n Recebeu o pacote: " + sentBlockNumber);

        if (sentBlockNumber != blockNum + 1) {
            return false;
        }

        //compara crc32
        return Util.checkCRC(bay);
    }

}