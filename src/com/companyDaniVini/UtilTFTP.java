package com.companyDaniVini;

import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class UtilTFTP {

	public static void puts(String s) {

		System.out.println(s);

	}

	public static void fatalError(String msg) {

		System.err.println("Fatal error: " + msg);
		System.exit(1);

	}

	public static long getCRC(byte[] bay){
		String string = new String(bay).trim();
		Checksum checksum = new CRC32();
		checksum.update(string.getBytes());
		return checksum.getValue();
	}

	public static boolean checkCRC(byte[] bay){
		//String mensagem = new String(bay);
		//System.out.println(mensagem + "\n");
		byte[] crcBytes = Arrays.copyOfRange(bay, 4, 14);
		//mensagem = mensagem.split("CRC:")[1];
		//String[] split = mensagem.split("Dados:");
//		String crc = split[0];
		String crc = new String(crcBytes).trim();
		long crcLDado = Long.parseLong(crc);
		String dados = new String(Arrays.copyOfRange(bay, 14, bay.length));
		long crcCalculado = getCRC(dados.getBytes());
		return crcLDado == crcCalculado;
	}


}
