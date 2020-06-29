package com.companyDaniVini;

import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class UtilTFTP {

    //metodo que gera o crc de um array de bytes
    public static long getCRC(byte[] bay) {
        String string = new String(bay).trim();
        Checksum checksum = new CRC32();
        checksum.update(string.getBytes());
        return checksum.getValue();
    }

    //compara os dados do pacote recebido com o crc que vem no pacote para ver se o pacote nao foi corrompido
    public static boolean checkCRC(byte[] bay) {
        //String mensagem = new String(bay);
        //System.out.println(mensagem + "\n");
        byte[] crcBytes = Arrays.copyOfRange(bay, 4, 14);
        String crc = new String(crcBytes).trim();
        long crcLDado = Long.parseLong(crc);
        String dados = new String(Arrays.copyOfRange(bay, 14, bay.length));
        long crcCalculado = getCRC(dados.getBytes());
        return crcLDado == crcCalculado;
    }


}
