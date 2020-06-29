package com.companyDaniVini.server;

import com.companyDaniVini.Util;

import java.net.*;
import java.io.*;

public class Server extends Thread {

    DatagramPacket pkt;
    final static int DEFAULT_PORT = 6969;

    public Server(DatagramPacket pkt) {
        this.pkt = pkt;
    }

    public void run() {
        byte[] buf = new byte[516];
        int port = pkt.getPort();
        InetAddress address = pkt.getAddress();

        try {
            // cria o socket
            DatagramSocket serverSocket = new DatagramSocket();

            //addShutdownHook
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    serverSocket.close();
                    System.out.println("Server Socket closed");
                }
            });
            // pega os dados do packet
            buf = pkt.getData();

            // pega o nome do arquivo dos dados do pacote
            String fileName = getFileName(buf);


            try {
                // verifica se o arquivo existe
                if (!new File(fileName).exists()) {
                    byte[] errorData = Util.makeErrorData(1, "Arquivo não encontrado.");
                    DatagramPacket errorPacket = new DatagramPacket(errorData, errorData.length, address, port);
                    serverSocket.send(errorPacket);
                    System.exit(1);
                }

                // abre reader no arquivo
                DataInputStream fileData = new DataInputStream(new FileInputStream(fileName));

                // comeca processo de mandar o arquivo
                Sender sender = new Sender(address, port, fileData, serverSocket);
                sender.send();

            } catch (Exception e) {
                System.out.println(e.getStackTrace());
            }
        } catch (Exception e) {
            System.out.println(e.getStackTrace());
        }
    }

    // pega o nome do arquivo de dentro do pacote
    public String getFileName(byte[] b) {
        String fileName = "";
        int i = 2;
        while (b[i] != 0) {
            fileName += (char) b[i++];
        }
        return fileName;
    }

    public static void main(String[] args) {

        try {
            System.out.println("Open Socket at: " + DEFAULT_PORT);
            DatagramSocket srv = new DatagramSocket(DEFAULT_PORT);

            while (true) {
                byte[] buf = new byte[516];
                DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                srv.receive(pkt);
                buf = pkt.getData();

                // cria thread pra lidar com as reqs e ficar ouvindo
                new Server(pkt).run();
            }
        } catch (Exception e) {
            System.err.println(e);
        }
    }
}
