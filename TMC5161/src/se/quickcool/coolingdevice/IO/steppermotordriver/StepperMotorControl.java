package se.quickcool.coolingdevice.IO.steppermotordriver;

/**
 * This class contains all the logic for controlling, operating and monitoring a
 * stepper motor using the TMC5161 motor controller. The motor is configured to
 * run at position mode with a certain configurable target velocity. All the
 * modes and constants used in this code are taken from the TMC5161 datasheet.
 * This class also acts as an interactive layer between the StepperMotor class
 * and the rest of the system.
 * 
 * <p>
 * <b>Note:</b> The TMC5161 is a motor controller designed by <i>Trinamics</i>
 * as a driver board for stepper motors. It only requires sending the proper
 * commands to it, as it will automatically take care of the rest.
 * </p>
 * 
 * @author Danial Mahmoud <<i>danial.mahmoud@quickcool.se</i>>
 *
 */
class StepperMotorControl {
	public static final int MICROSTEPS_PER_REVOLUTION = 51200; // according to TMC datasheet p. 59

	/*
	 * The values below correspond to the constants and register values contained in
	 * the TMC API release available for download at:
	 * https://www.trinamic.com/support/software/access-package/
	 */
	private byte TMC5161_GCONF = 0x00; // RW: General Configuration Register
	private byte TMC5161_IFCNT = 0x02; // R: Interface transmission counter - incr. with each successful UART write
										// access
	private byte TMC5161_SLAVECONF = 0x03;
	private byte TMC5161_IHOLD_IRUN = 0x10; // W: Driver current control
	private byte TMC5161_TPOWERDOWN = 0x11; // W: sets delay time after standstill for motor current to power down
	private byte TMC5161_TPWMTHRS = 0x13; // W: upper velocity for stealthChop voltage PWM mode
	private byte TMC5161_RAMPMODE = 0x20;
	private byte TMC5161_XACTUAL = 0x21;
	private byte TMC5161_VACTUAL = 0x22; // R: Actual motor velocity (signed), sign matches motion direction
	private byte TMC5161_VSTART = 0x23;
	private byte TMC5161_A1 = 0x24;
	private byte TMC5161_V1 = 0x25;
	private byte TMC5161_AMAX = 0x26;
	private byte TMC5161_VMAX = 0x27;
	private byte TMC5161_DMAX = 0x28;
	private byte TMC5161_D1 = 0x2A;
	private byte TMC5161_VSTOP = 0x2B;
	private byte TMC5161_XTARGET = 0x2D;
	private byte TMC5161_CHOPCONF = 0x6C; // RW: chopper and driver configuration
	private byte TMC5161_COOLCONF = 0x6D; // W: stallGuard configuration
	private byte TMC5161_DRVSTATUS = 0x6F; // R: stallGuard status and driver error flags

	private byte TMC5161_MODE_POSITION = 0; // for position mode write 0 to Rampmode reg.

	private MotorDriverCommunication mdc;
	private PackageAnalyzer pa;

	private boolean enableStallguardException;
	private boolean enableVelocityException;

	private double desiredRPS;

	private static final double MIN_VELOCITY = 0, MAX_VELOCITY = 5;

	/**
	 * Class constructor that creates instances of the class that analyzes incoming
	 * packages and the one that handles their synthesis and shifts them to the
	 * transportation layer.
	 */
	StepperMotorControl() {
		this.pa = new PackageAnalyzer();
		this.mdc = new MotorDriverCommunication();
	}

	/**
	 * Initialize the stepper motor with certain configuration values. Some of these
	 * values are read back to ensure a correct configuration. This also indirectly
	 * tells us if the TMC-board is alive or not. A control logic in the form of an
	 * <i>if-else-statement</i> is implemented to make sure input speed does not
	 * exceed a certain predetermined interval.
	 * 
	 * @param rps
	 * @throws CommunicationException
	 * @throws DriverErrorException
	 */
	void initStepperMotor(double rps) throws CommunicationException, DriverErrorException {
		this.desiredRPS = rps;
		mdc.initializeSerialPort();
		if (rps >= MIN_VELOCITY && rps <= MAX_VELOCITY) {
			TMC5161Configuration(rps);
			boolean readBackOk = readBackConfiguredValues();
			if (!readBackOk) {
				throw new DriverErrorException("Configuration values could not be read back correctly");
			} else {
				System.out.println("Configuration ok");
			}

		} else {
			throw new DriverErrorException("Unacceptable velocity value");
		}
	}

	/**
	 * Function that attempts to close serial port.
	 * 
	 * @return true if successful, otherwise false
	 * @throws CommunicationException
	 */
	boolean closeSerialPort() throws CommunicationException {
		if (!mdc.closeSerialPort()) {
			throw new CommunicationException("Failed to close serial port");
		}
		return true;
	}

	private boolean readBackConfiguredValues() throws CommunicationException {
		// Unfortunately not all the configuration registers are readable ... =(
		System.out.println("Read back GCONF");
		byte[] dataBytesGCONF = mdc.sendReadAccessPackage(TMC5161_GCONF);
		if (dataBytesGCONF == null || dataBytesGCONF.length != 8
				|| (dataBytesGCONF.length == 8 && !extractDatagram(dataBytesGCONF).equals("0x0000000C"))) {
			return false;
		}
		delayMillis(3);
		System.out.println("Read back CHOPCONF");
		byte[] dataBytesCHOPCONF = mdc.sendReadAccessPackage(TMC5161_CHOPCONF);
		if (dataBytesCHOPCONF == null || dataBytesCHOPCONF.length != 8
				|| (dataBytesCHOPCONF.length == 8 && !extractDatagram(dataBytesCHOPCONF).equals("0x000100C3"))) {
			return false;
		}
		delayMillis(3);
		System.out.println("Read back XACTUAL");
		byte[] dataBytesXACTUAL = mdc.sendReadAccessPackage(TMC5161_XACTUAL);
		if (dataBytesXACTUAL == null || dataBytesXACTUAL.length != 8
				|| (dataBytesXACTUAL.length == 8 && !extractDatagram(dataBytesXACTUAL).equals("0x00000000"))) {
			return false;
		}
		delayMillis(3);
		System.out.println("Read back RAMPMODE");
		byte[] dataBytesRAMPMODE = mdc.sendReadAccessPackage(TMC5161_RAMPMODE);
		if (dataBytesRAMPMODE == null || dataBytesRAMPMODE.length != 8
				|| (dataBytesRAMPMODE.length == 8 && !extractDatagram(dataBytesRAMPMODE).equals("0x00000000"))) {
			return false;
		}

		return true;
	}

	private String extractDatagram(byte[] replyPackage) {
		int msb = 3, lsb = 6;
		StringBuilder sb = new StringBuilder();
		sb.append("0x");
		for (int i = msb; i <= lsb; i++) {
			int currByte = Byte.toUnsignedInt(replyPackage[i]);
			if (currByte <= 15) {
				sb.append("0" + Integer.toHexString(currByte).toUpperCase());
			} else {
				sb.append("" + Integer.toHexString(currByte).toUpperCase());
			}
		}
		System.out.println("databytes read back: " + sb.toString());
		return sb.toString();
	}

	private void delayMillis(int delayTimeMillis) {
		try {
			Thread.sleep(delayTimeMillis);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Initialize the stepper motor driver registers. The values written to these
	 * registers configure among other things the address of the device, the current
	 * to the motor and its initial target velocity. Read the comments below for
	 * further details. The values are pre-configured and are not meant to be
	 * changed "on-the-fly" (during normal motor operation) except for the target
	 * velocity.
	 * 
	 * @param serialPort The serial port through which communication with the
	 *                   TMC5161 occurs
	 * @param rps        Start velocity in <i>Rotation per Second</i>
	 */
	void TMC5161Configuration(double rps) {
		System.out.println("Initialize motor drivers: \n");

		mdc.sendWriteAccessPackage(TMC5161_GCONF, 0x0000000C);
		mdc.sendWriteAccessPackage(TMC5161_CHOPCONF, 0x000100C3);

		// Configure stallGuard
		mdc.sendWriteAccessPackage(TMC5161_COOLCONF, 0x00000000);

		/*
		 * IHOLD (bit 0-4) = 1 ==> standby current = 0.24 A, IRUN (bit 8-12) = 2 ==> RMS
		 * motor current = 0.35 A. These values can be looked up in the TMC IDE.
		 * 
		 * Note that lower current means lower torque.
		 */
		mdc.sendWriteAccessPackage(TMC5161_IHOLD_IRUN, 0x00080201);

		mdc.sendWriteAccessPackage(TMC5161_TPOWERDOWN, 0x0000000A);
		mdc.sendWriteAccessPackage(TMC5161_TPWMTHRS, 0x000001F4);

		// Reset positions
		mdc.sendWriteAccessPackage(TMC5161_XTARGET, 0);
		mdc.sendWriteAccessPackage(TMC5161_XACTUAL, 0);

		double VMAX = MICROSTEPS_PER_REVOLUTION * rps; // VMAX = target velocity

		mdc.sendWriteAccessPackage(TMC5161_VSTART, 1); // VMAX must >= VSTART
		mdc.sendWriteAccessPackage(TMC5161_A1, 250);
		mdc.sendWriteAccessPackage(TMC5161_V1, 50000);
		mdc.sendWriteAccessPackage(TMC5161_AMAX, 250);
		mdc.sendWriteAccessPackage(TMC5161_VMAX, (int) VMAX); // set target velocity
		mdc.sendWriteAccessPackage(TMC5161_DMAX, 250);
		mdc.sendWriteAccessPackage(TMC5161_D1, 250);
		mdc.sendWriteAccessPackage(TMC5161_VSTOP, 2); // VSTOP must >= VSTART
		mdc.sendWriteAccessPackage(TMC5161_RAMPMODE, TMC5161_MODE_POSITION);
	}

	/**
	 * Disables the motor driver by a software write. To be used in case of certain
	 * errors such as undervoltage or overtemperature which requires, in addition to
	 * an automatic hardware disable, a disabling by software by clearing TOFF bits
	 * in the CHOPCONF register. The motor stops running.
	 * 
	 * <p>
	 * <b>Note:</b> This function <i>must</i> be invoked if any of the following
	 * errors occur: open load or short-to-ground (on both phases)
	 * </p>
	 */
	void disableDriver() {
		mdc.sendWriteAccessPackage(TMC5161_CHOPCONF, 0x000100C0); // TOFF = 0 disables drivers (see
																	// p. 48)
	}

	/**
	 * The motor runs in position mode. The reason for this is because we wish to
	 * trick the motor into a "carrot and stick" behavior, where the motor never
	 * reaches the target position. Its indispensable that the position counter,
	 * which represents the actual position of the motor, is reset before the target
	 * position is reached. Through this we emulate <i>velocity mode</i> with the
	 * only advantage that when communication is terminated with the TMC5161 the
	 * motor eventually stops running. This was not the case when using velocity
	 * mode, wherein the motor continued running even after program execution was
	 * terminated.
	 * <p>
	 * <b>NOTE:</b> The target position is only reached when communication with the
	 * TMC ceases, after which the motor slows down and eventually stops completely
	 * within only a few seconds.
	 * </p>
	 * 
	 * @param targetPos unreachable target position during normal operation
	 */
	void rotateToTargetPosition(long targetPos) {
		System.out.println("Rotate to target pos");
		mdc.sendWriteAccessPackage(TMC5161_XTARGET, targetPos); // rotate to target position, starts motor
	}

	/**
	 * Resets the position counter which increments with every motor step. Resetting
	 * this counter is meant to prevent the stepper motor from reaching the target
	 * position and forcing it to restart its rotation as if it was programmed to
	 * run in <i>velocity mode</i> thus rotating continuously.
	 * 
	 */
	void resetPositionCounter() {
		mdc.sendWriteAccessPackage(TMC5161_XACTUAL, 0);
	}

	/**
	 * Power cycle 5 V VCC_IO pin on the TMC board that resets the chip. For the
	 * time being it is a dummy function that does nothing except return. It will be
	 * replaced with code in later version of this program.
	 */
	public void powerCycle() {
		// empty dummy function
		return;
	}

	/**
	 * Sets a new target velocity for the stepper motor by writing to the rampmode
	 * register called <i>VMAX</i>. The rps value is converted to microsteps by
	 * multiplication with the number of microsteps we have per full revolution.
	 * <p>
	 * <b>Note:</b> The value can be changed during motion.
	 * </p>
	 * 
	 * @param rps new target velocity given in <i>Rotation per Second</i>
	 * @throws DriverErrorException
	 */
	void setNewTargetVelocity(double rps) throws DriverErrorException {
		this.desiredRPS = rps;
		if (rps >= MIN_VELOCITY && rps <= MAX_VELOCITY) {
			double VMAX = MICROSTEPS_PER_REVOLUTION * rps;
			mdc.sendWriteAccessPackage(TMC5161_VMAX, (int) VMAX);
		} else {
			throw new DriverErrorException("Unacceptable velocity value");
		}
	}

	/**
	 * This function can be viewed as an interface function for the StepperMotor
	 * class which can freely invoke it whenever interested in the current status of
	 * the motor such as checking if a stall has occurred, motor velocity etc. by
	 * sending a Read Access request inquiring the contents of desired status
	 * register. There is a common list of important statuses declared in the
	 * StepperMotor class that are publicly accessible.
	 * <p>
	 * This method needs to synchronize with the event-based callback function which
	 * asynchronously receives incoming data bytes. This is the item that is shared
	 * between the threads, and hence they need to coordinate access to it (global
	 * variable).
	 * <p>
	 * 
	 * @param serialPort The serial port through which communication with the TMC
	 *                   occurs
	 * @param status     Requested motor status
	 * @return Instance of the class MotorStatus containing relevant info
	 * @throws DataCorruptException
	 * @throws DriverErrorException
	 */
	void checkMotorStatus() throws CommunicationException, DataCorruptException, DriverErrorException {
		MotorStatus motorStatus;
		byte[] databytesVelocity, databytesDriverError;

		// velocity status is not in same register as the other statuses so we need to
		// inquire separately
		databytesVelocity = mdc.sendReadAccessPackage(TMC5161_VACTUAL);

		int tempActualVel = pa.analyzeReplyPackage(StepperMotor.ACTUAL_VELOCITY_STATUS, databytesVelocity)
				.getActualVelocityStatus();

		// Now inquire driver error status
		databytesDriverError = mdc.sendReadAccessPackage(TMC5161_DRVSTATUS);

		motorStatus = pa.analyzeReplyPackage(StepperMotor.DRIVER_ERROR_STATUS, databytesDriverError);
		motorStatus.setActualVelocityStatus(tempActualVel);

		if (motorStatus.getDataCorruptStatus() == true) {
			throw new DataCorruptException("No motor status available");
		}

		// Velocity status
		int actualVelocity = motorStatus.getActualVelocityStatus();
		if (actualVelocity >= 0) {
			double upperVelocityLimit = 1.1 * ((double) this.desiredRPS);
			double lowerVelocityLimit = 0.95 * ((double) this.desiredRPS);
			if (enableVelocityException) {
				if (actualVelocity <= lowerVelocityLimit) {
					throw new DriverErrorException("Motor velocity too low");
				} else if (actualVelocity >= upperVelocityLimit) {
					throw new DriverErrorException("Motor velocity too high");
				}
			} // else just continue ...
			int microSteps = StepperMotorControl.MICROSTEPS_PER_REVOLUTION;
			System.out.println("Actual velocity: " + actualVelocity + " microsteps ===> "
					+ Double.parseDouble(String.valueOf(actualVelocity)) / microSteps + " RPS");
		}

		// Stallguard status
		if (motorStatus.getStallGuardStatus()) {
			if (enableStallguardException) {
				throw new DriverErrorException("Stall detected!");
			} // else do nothing ...
		} else {
			System.out.println("no stall");
		}

		// Overtemperature prewarning status
		if (motorStatus.getOverTemperaturePrewarningStatus()) {
			throw new DriverErrorException("Overtemperature pre-warning threshold is exceeded");
		} else {
			System.out.println("no overtemperature pre-warning threshold has been exceeded");
		}

		// Overtemperature status
		if (motorStatus.getOverTemperatureStatus()) {
			throw new DriverErrorException("Overtemperature!");
		} else {
			System.out.println("no overtemperature");
		}

		// Open load indicator phase A
		if (motorStatus.getOpenLoadIndicatorStatus("Phase A")) {
			throw new DriverErrorException("Open load detected on Phase A!");
		} else {
			System.out.println("no open load detected on Phase A");
		}

		// Open load indicator phase B
		if (motorStatus.getOpenLoadIndicatorStatus("Phase B")) {
			throw new DriverErrorException("Open load detected on Phase B!");
		} else {
			System.out.println("no open load detected on Phase B");
		}

		// Short to ground indicator phase A
		if (motorStatus.getShortToGroundIndicatorStatus("Phase A")) {
			throw new DriverErrorException("Short to ground detected on Phase A!");
		} else {
			System.out.println("no short to ground detected on Phase A");
		}

		// Short to ground indicator phase B
		if (motorStatus.getShortToGroundIndicatorStatus("Phase B")) {
			throw new DriverErrorException("Short to ground detected on Phase B!");
		} else {
			System.out.println("no short to ground detected on Phase B");
		}
	}

	/**
	 * Enables the possibility to throw an exception as a result of motor exceeding
	 * allowable velocity limits. Gives the user the option to enable or disable
	 * velocity monitoring. Gets invoked by higher level function. The variable that
	 * is set, is checked some place else.
	 * 
	 * @param enableVelocity
	 */
	public void enableStallguardException(boolean enableStallguardException) {
		this.enableStallguardException = enableStallguardException;
	}

	/**
	 * Enables the possibility to throw an exception as a result of motor exceeding
	 * allowable velocity limits. Gives the user the option to enable or disable
	 * velocity monitoring. Gets invoked by higher level function. The variable that
	 * is set, is checked some place else.
	 * 
	 * @param enableVelocity
	 */
	public void enableVelocityException(boolean enableVelocityException) {
		this.enableVelocityException = enableVelocityException;

	}
}
