package se.quickcool.coolingdevice.IO.steppermotordriver;

/**
 * This class is responsible for organizing the packages that are to be sent to
 * the TMC. Communication occurs from a serial port on the PC to the UART single
 * wire interface on the TMC. Read and write access packages are formed
 * according to the datagram structure specified in the TMC5161 datasheet.
 * <p>
 * <b>Note:</b> JSerialComm is a Java library designed to enable communication
 * with a serial port using Java.
 * </p>
 * 
 * @author Danial Mahmoud <<i>danial.mahmoud@quickcool.se</i>>
 *
 */
class MotorDriverCommunication {
	private byte SLAVEADDRESS = 0x00;
	private UARTSerialComm usc;
	private byte[] newData; // globally accessible buffer for data arrived at the serial port

	MotorDriverCommunication() {
		this.usc = new UARTSerialComm(this);
	}

	/**
	 * The function below puts together the UART-datagrams that are to be send for
	 * <i>Write Access</i> to TMC5161 registers. It lastly invokes a function in the
	 * lower layers that physically sends the package. The datagram package is
	 * structured according to the structure outlined in the TMC datasheet. The CRC
	 * checksum of the package is computed as a control measure to verify intact
	 * data arrival. The term <i>Write Access</i> refers to a request to write data
	 * contained in the transferred package to specific TMC5161 registers for
	 * enabling various kinds of control or operations of the stepper motor.
	 * <p>
	 * <b>Note:</b> The TMC5161 takes a 64-bit data package for Write Access: 8 sync
	 * + reserved, 8 slave address, 8 register address, 32-bit data, 8 CRC
	 * </p>
	 * 
	 * @param registerAddress The address of the register that is to be written to
	 * @param datagram        32-bit data value which to be written to the register
	 *                        address
	 */
	void sendWriteAccessPackage(byte registerAddress, long datagram) {
		byte CRC = 0;
		byte[] buf = new byte[8];
		CRC = CRCgenerator.nextCRC(CRC, (byte) 0x05);
		CRC = CRCgenerator.nextCRC(CRC, SLAVEADDRESS);
		CRC = CRCgenerator.nextCRC(CRC, (byte) (registerAddress | 0x80));
		CRC = CRCgenerator.nextCRC(CRC, (byte) ((datagram >> 24) & 0xff));
		CRC = CRCgenerator.nextCRC(CRC, (byte) ((datagram >> 16) & 0xff));
		CRC = CRCgenerator.nextCRC(CRC, (byte) ((datagram >> 8) & 0xff));
		CRC = CRCgenerator.nextCRC(CRC, (byte) (datagram & 0xff));

		buf[0] = 0x05;
		buf[1] = SLAVEADDRESS;
		buf[2] = (byte) (Byte.toUnsignedInt(registerAddress) | 0x80); // Add 0x80 to the reg.addr. for write accesses!
		buf[3] = (byte) ((datagram >> 24) & 0xFF);
		buf[4] = (byte) ((datagram >> 16) & 0xFF);
		buf[5] = (byte) ((datagram >> 8) & 0xFF);
		buf[6] = (byte) (datagram & 0xFF);
		buf[7] = CRC;

		usc.uartWriteAccess(registerAddress, buf);
	}

	/**
	 * The function below puts together the UART-datagrams that are to be sent to
	 * the TMC5161 for Read Access purposes. <i>Read Access</i> refers to a request
	 * to read from readable TMC5161 registers. If the package is valid, this will
	 * trigger a reply from the TMC5161. The reply package that gets returned will
	 * contain the value of the read register.
	 * 
	 * @param registerAddress The address of the register that is to be read from
	 * @throws CommunicationException
	 */
	synchronized byte[] sendReadAccessPackage(byte registerAddress) throws CommunicationException {
		byte CRC = 0;
		byte[] buf = new byte[4];

		CRC = CRCgenerator.nextCRC(CRC, (byte) 0x05);
		CRC = CRCgenerator.nextCRC(CRC, SLAVEADDRESS);
		CRC = CRCgenerator.nextCRC(CRC, registerAddress);

		buf[0] = 0x05;
		buf[1] = SLAVEADDRESS;
		buf[2] = registerAddress;
		buf[3] = CRC;

		usc.uartReadAccess(registerAddress, buf);
		try {
			wait(100);
		} catch (InterruptedException e) {
			throw new CommunicationException("Waiting time expired: no reply answer");
		}

		return newData;
	}

	/**
	 * Higher layer function that invokes lower layer function that initializes the
	 * serial port with various parameters.
	 * 
	 * @throws CommunicationException
	 */
	void initializeSerialPort() throws CommunicationException {
		usc.initSerialPort();
	}

	/**
	 * Function that tries to close the serial port through which communication
	 * occurs.
	 * 
	 * @return true if port successfully closed, false otherwise
	 */
	boolean closeSerialPort() {
		return usc.closeSerialPort();
	}

	/**
	 * This function is used outside this class in a callback-function to set a
	 * globally accessible function.
	 * 
	 * @param newData reply package from the TMC
	 */
	synchronized void setNewData(byte[] newData) {
		this.newData = newData;
		notifyAll(); // wake up functions that are waiting (wait())
	}

}
