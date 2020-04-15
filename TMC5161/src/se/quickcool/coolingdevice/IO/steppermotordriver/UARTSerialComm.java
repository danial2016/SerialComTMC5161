package se.quickcool.coolingdevice.IO.steppermotordriver;

import java.io.IOException;
import java.io.OutputStream;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

/**
 * This class is responsible for sending the physical bytes across the serial
 * interface to the TMC using a library called JSerialComm which enables serial
 * communication from within Java.
 * 
 * @author Danial Mahmoud <<i>danial.mahmoud@quickcool.se</i>>
 * @version 1.0
 */
class UARTSerialComm implements SerialPortDataListener {
	private SerialPort serialPort;
	private MotorDriverCommunication mdc;

	UARTSerialComm(MotorDriverCommunication mdc) {
		this.mdc = mdc;
	}

	/**
	 * This function initializes the serial port through which communication with
	 * the TMC occurs. The serial port is then configured with baud rate and stop
	 * bits after which it is opened. Lastly, the callback function is added to this
	 * class.
	 * 
	 * @throws CommunicationException
	 */
	void initSerialPort() throws CommunicationException {
		System.out.println("\nUsing Library Version v" + SerialPort.getVersion());

		SerialPort[] ports = SerialPort.getCommPorts();

		if (ports.length == 0) {
			throw new CommunicationException("\nNo available ports\n");
		} else {
			System.out.println("\nAvailable Ports:\n");
		}

		for (int i = 0; i < ports.length; ++i) {
			System.out.println(
					"   [" + i + "] " + ports[i].getSystemPortName() + ": " + ports[i].getDescriptivePortName());
		}

		// We are using the Host OS serial port (COM2) which we have mapped to
		// "/dev/ttyS1" inside Virtual Box
		SerialPort serialPort = ports[0]; // ttyS1
		serialPort.setBaudRate(115200);
		serialPort.setNumStopBits(SerialPort.ONE_STOP_BIT);

		boolean opened = serialPort.openPort();
		System.out.println("\nOpening " + serialPort.getSystemPortName() + ": " + opened);

		if (!opened) {
			throw new CommunicationException("Failed to open COM-port");
		} else {
			System.out.println("COM-port is open");
		}

		this.serialPort = serialPort;
		serialPort.addDataListener(this);
	}

	/**
	 * Function that tries to close the serial port through which communication
	 * occurs.
	 * 
	 * @return true if port successfully closed, false otherwise
	 */
	boolean closeSerialPort() {
		return serialPort.closePort();
	}

	@Override
	/**
	 * Returns the serial event we that the callback functions triggers on which in
	 * our case is that data is available at the port.
	 * 
	 * @return serial event stating that data is available at the serial port
	 */
	public int getListeningEvents() {
		return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
	}

	/**
	 * The function below is the event-based callback-function that is triggered
	 * every time there is any data available at the serial port after a Read Access
	 * request has been sent to the TMC.
	 * <p>
	 * For ordinary synchronized methods, the lock will be the object on which the
	 * method is being invoked, in this case the method is invoked on the class
	 * StepperMotorControl. Every Java object has an associated lock. Java locks can
	 * be held by no more than one thread at a time. The thread has to acquire the
	 * lock. Only one thread executes synchronized code protected by a given lock at
	 * one time.
	 * </p>
	 */
	@Override
	public void serialEvent(SerialPortEvent event) {
		/*
		 * The input parameter, SerialPortEvent, is a class that describes an
		 * asynchronous serial port event. The callback function receives an already
		 * created object that is an instance of this class. The object contains all the
		 * info about the event.
		 */
		if (event.getEventType() == SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
			byte[] newData = new byte[serialPort.bytesAvailable()];

			int numRead = serialPort.readBytes(newData, newData.length);
			if (numRead == 4 || numRead == 8) {
				System.out.println("\nRead " + numRead + " available bytes.");
				System.out.println("Received bytes: " + "[" + getDataBytesString(newData) + "]");
			}

			mdc.setNewData(newData);
		}
	}

	private String getDataBytesString(byte[] data) {
		StringBuilder res;
		String dataBytes = "";
		for (int i = 0; i < data.length; i++) {
			res = new StringBuilder(Integer.toHexString(Byte.toUnsignedInt(data[i])).toUpperCase());
			if (i < 7) {
				if (res.length() == 1) {
					dataBytes += "0x0" + Integer.toHexString(Byte.toUnsignedInt(data[i])).toUpperCase() + ", ";
				} else {
					dataBytes += "0x" + Integer.toHexString(Byte.toUnsignedInt(data[i])).toUpperCase() + ", ";
				}
			}
			if (i >= 7) {
				if (res.length() == 1) {
					dataBytes += "0x0" + Integer.toHexString(Byte.toUnsignedInt(data[i])).toUpperCase();
				} else {
					dataBytes += "0x" + Integer.toHexString(Byte.toUnsignedInt(data[i])).toUpperCase();
				}
			}
		}
		return dataBytes;
	}

	/**
	 * The function below physically transfers the UART-datagrams to the motor
	 * driver across a serial interface requiring <i>Write Access</i> to TMC5161
	 * registers using JSerialComm library functions. The term <i>Write Access</i>
	 * refers to a request to write data contained in the transferred package to
	 * specific TMC5161 registers for enabling various kinds of control or
	 * operations of the stepper motor. The datagram package is structured according
	 * to the structure outlined in the TMC datasheet. The CRC checksum of the
	 * package is computed as a control measure to verify intact data arrival.
	 * <p>
	 * <b>Note:</b> The TMC5161 takes a 64-bit data package for Write Access: 8 sync
	 * + reserved, 8 slave address, 8 register address, 32-bit data, 8 CRC
	 * </p>
	 * 
	 * @param registerAddress The address of the register that is to be written to
	 * @param datagram        32-bit data value which to be written to the register
	 *                        address
	 */
	void uartWriteAccess(byte registerAddress, byte[] buf) {
		serialPort.writeBytes(buf, buf.length); // write datagram for Write Access
		System.out.println("Sent bytes: " + "[" + getDataBytesString(buf) + "]");

		OutputStream os = serialPort.getOutputStream();
		try {
			os.flush(); // wait until all data are written
			os.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		delayMillis(5); // TODO normal is 5, unfortunately not possible to reduce or remove delay
	}

	/**
	 * The function below physically transfers the UART-datagrams across the serial
	 * interface to the TMC5161 for Read Access purposes using JSerialComm library
	 * functions. <i>Read Access</i> refers to a request to read from readable
	 * TMC5161 registers. If the package is valid, this will trigger a reply from
	 * the TMC5161. The reply package which will contain the value of the read
	 * register is received in the callback function.
	 * 
	 * @param buf             The byte buffer that is to be sent to the TMC
	 * @param registerAddress The address of the register that is to be read from
	 */
	void uartReadAccess(byte registerAddress, byte[] buf) {
		OutputStream os = serialPort.getOutputStream();

		serialPort.writeBytes(buf, buf.length); // write datagram for Read Access

		try {
			os.flush(); // wait until all data is written
			os.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void delayMillis(int delayTimeMillis) {
		try {
			Thread.sleep(delayTimeMillis);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
