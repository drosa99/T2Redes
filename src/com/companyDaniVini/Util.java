package com.companyDaniVini;

import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class Util {

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


    //converte os primeiros 2 bytes to block number
    public static int getBlockNumber(byte[] bay) {
        int highByte = bay[2] & 0xff;
        highByte = highByte << 8;
        return highByte | (bay[3] & 0xff);
    }

    public static byte[] makeErrorData(int errorCode, String errorMessage) {
		// retorna a mensagem de erro
		int position;
		byte[] errorBytes = new byte[516];
		errorBytes[0] = 0;
		errorBytes[1] = 5;
		errorBytes[2] = 0;
		errorBytes[3] = (byte) errorCode;

		for (position = 0; position < errorMessage.length(); position++) {
			errorBytes[4 + position] = (byte) errorMessage.charAt(position);
		}
		errorBytes[position + 4] = 0;

		return errorBytes;
	}
}
