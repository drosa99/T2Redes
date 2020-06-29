package com.companyDaniVini;

public class PacketTFTP {



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
