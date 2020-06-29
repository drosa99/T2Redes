package com.companyDaniVini.client;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Client {

    final static int DEFAULT_PORT = 6969;

    public static void main(String[] args) {

        final String dir = System.getProperty("user.dir") + "/src/com/companyDaniVini";
        System.out.println("current dir = " + dir);

        byte[] bay = new byte[526];
        String server = "localhost";
        String name = args[0];
        String fileName = System.getProperty("user.dir") + "/src/com/companyDaniVini/origem/" + name;

        try {
            // preparar mensagem que manda o nome do arquivo
            Client.prepareDataRRQ(bay, fileName, "octet");

            //mandar para o server qual o arquivo que ele quer
            DatagramSocket cliSkt = new DatagramSocket();

            InetAddress ipAddr = InetAddress.getByName(server);
            int port = DEFAULT_PORT;
            DatagramPacket pkt = new DatagramPacket(bay, bay.length, ipAddr, port);

            cliSkt.send(pkt);

            //cria arquivo que será transferido na pasta destino, se o arquivo já existe, é sobrescrito
            File file = new File("./destino/" + name);
            DataOutputStream fileOut = new DataOutputStream(new FileOutputStream(file));

            //recebe a quantidade de pacotes que serao enviados
            DatagramPacket tampkt = new DatagramPacket(new byte[8], 8, ipAddr, port);
            cliSkt.receive(tampkt);
            port = tampkt.getPort();
            ipAddr = tampkt.getAddress();
            int qntdPacotes = Integer.parseInt(new String(tampkt.getData()).trim());
            System.out.println("Recebeu a quantidade de pacotes do arquivo: " + qntdPacotes);

            //manda ack da quantidade de blocos do arquivo
            DatagramPacket ackPkt = new DatagramPacket(tampkt.getData(), tampkt.getData().length, ipAddr, port);
            System.out.println("Vai mandar ACK da quantidade de pacotes do arquivo");
            cliSkt.send(ackPkt);

            //recebe o primeiro bloco
            //os blocos serao compostos por 526 bytes -> contendo numero do bloco + crc + 512 bytes do arquivo
            bay = new byte[526];
            pkt = new DatagramPacket(bay, bay.length, ipAddr, port);
           // int blockNum = ReceiverUtils.handlePacket(cliSkt, pkt, fileOut, ipAddr, port, 0, pkt.getLength());

            //comeca processo de receber pacotes
            Receiver receiver = new Receiver(ipAddr, port, fileOut, cliSkt, 0, qntdPacotes);
            receiver.receive();

        } catch (Exception e) {
            System.out.println(e);
        }


    }

    public static void prepareDataRRQ(byte[] bay, String fileName, String mode) {
        int index;
        // seta os primeiros 2 bits 01 para indicar RRQ
        bay[0] = 0;
        bay[1] = 1;

        //popula próximos bytes livres com filename seguido de bytes nulos
        for (index = 0; index < fileName.length(); index++) {
            bay[index + 2] = (byte) fileName.charAt(index);
        }
        index += 2;
        bay[index++] = 0;

        //popula os próximos bytes com octect seguido de bytes nulos
        for (int i = 0; i < mode.length(); i++) {
            bay[index++] = (byte) mode.charAt(i);
        }
        bay[index] = 0;
    }
}
