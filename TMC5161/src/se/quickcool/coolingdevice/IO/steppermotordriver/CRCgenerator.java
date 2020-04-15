package se.quickcool.coolingdevice.IO.steppermotordriver;

/**
 * This class contains static functions that computes CRC checksums.
 * 
 * @author Danial Mahmoud <<i>danial.mahmoud@quickcool.se</i>>
 * @version 1.0
 */
class CRCgenerator {
	private static final byte CRC8_GEN = 0x07; // CRC generator polynomial

	/**
	 * Computes CRC of data input parameter.
	 * 
	 * @param crc
	 * @param data
	 * @return result of computation
	 */
	static byte nextCRC(byte crc, byte data) {
		byte temp = crc;

		for (int i = 0; i <= 7; i++) {
			temp = nextCRCSingle(temp, data, CRC8_GEN, (byte) i);
		}

		return temp;
	}

	/**
	 * The purpose of this function is to run a CRC check on the reply package from
	 * the TMC. The result of the computation will be compared to the actual CRC
	 * byte in the reply package. If they are not identical it will be assumed that
	 * a data corruption has occurred in the received package.
	 * 
	 * @param replyPackage The incoming data package from the TMC
	 * @return computed CRC byte
	 */
	static byte runCRCcheck(byte[] replyPackage) {
		byte CRC = 0;
		CRC = nextCRC(CRC, replyPackage[0]);
		CRC = nextCRC(CRC, replyPackage[1]);
		CRC = nextCRC(CRC, replyPackage[2]);
		CRC = nextCRC(CRC, replyPackage[3]);
		CRC = nextCRC(CRC, replyPackage[4]);
		CRC = nextCRC(CRC, replyPackage[5]);
		CRC = nextCRC(CRC, replyPackage[6]);
		return CRC;
	}

	private static byte nextCRCSingle(byte crc, byte data, byte gen, byte bit) {
		byte compare;

		compare = (byte) (Byte.toUnsignedInt(data) << (7 - Byte.toUnsignedInt(bit)));
		compare = (byte) (Byte.toUnsignedInt(compare) & 0x80);

		if (((Byte.toUnsignedInt(crc) & 0x80) ^ (Byte.toUnsignedInt(compare))) != 0) {
			return (byte) ((Byte.toUnsignedInt(crc) << 1) ^ Byte.toUnsignedInt(gen));
		} else {
			return (byte) (Byte.toUnsignedInt(crc) << 1);
		}
	}

}
