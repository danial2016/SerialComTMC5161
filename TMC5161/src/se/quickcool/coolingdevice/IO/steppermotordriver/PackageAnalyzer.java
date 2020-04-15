package se.quickcool.coolingdevice.IO.steppermotordriver;

/**
 * This class is responsible for analyzing incoming data packages and returning
 * a MotorStatus-object with the relevant status parameters set.
 * 
 * @author Danial Mahmoud <<i>danial.mahmoud@quickcool.se</i>>
 * @version 1.0
 */

class PackageAnalyzer {
	private MotorStatus ms;

	PackageAnalyzer() {
		ms = new MotorStatus();
	}

	private int convertHexToInt(byte[] data) {
		// Lowest byte begins from farthest right: e.g. 32-bit data: ... [0xB3 | 0xB2 |
		// 0xB1 | 0xB0] ...
		StringBuilder hexData = new StringBuilder();
		int sum = 0;

		// for-loop below gives a 2-number representation of a byte, e.g. "00" instead
		// of just "0"
		for (int i = data.length - 1; i >= 0; i--) {
			// TODO Actual velocity can be negative (signed), which represents motor
			// direction!
			StringBuilder res = new StringBuilder(Integer.toHexString(Byte.toUnsignedInt(data[i])).toUpperCase());
			if (res.length() == 1) {
				res.insert(0, "0");
				hexData.insert(0, res);
			} else if (res.length() == 2) {
				hexData.insert(0, res);
			}
		}
		String[] hexaNumbers = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F" };
		String hexDataInString = hexData.toString();
		for (int i = hexDataInString.length() - 1; i >= 0; i--) {
			for (int k = 0; k < hexaNumbers.length; k++) {
				if (Character.toString(hexDataInString.charAt(i)).equals(hexaNumbers[k])) {
					sum += k * Math.pow(16, 7 - i);
					break;
				}
			}
		}
		return sum;
	}

	private MotorStatus analyzeReplyData(int status, byte regAddr, byte[] replyPackage) {
		int FLAG_ACTIVE = 1;

		byte[] data = new byte[4]; // 32-bit data contained in the Reply Package

		data[0] = replyPackage[3]; // MSB
		data[1] = replyPackage[4]; // ...
		data[2] = replyPackage[5]; // ...
		data[3] = replyPackage[6]; // LSB

		byte mostSignificantByte = data[0];

		/*
		 * Refer to page 52 of TMC5161 datasheet for status and error flags and bits
		 */
		switch (regAddr) {
		// TODO change case names to abstract symbolic name
		case 0x22: // TMC5161_VACTUAL
			if (status == StepperMotor.ACTUAL_VELOCITY_STATUS) {
				int actualVelocity = convertHexToInt(data);
				ms.setActualVelocityStatus(actualVelocity);
			}
			break;
		case 0x6F: // TMC5161_DRVSTATUS
			if (status == StepperMotor.DRIVER_ERROR_STATUS) {
				// stallguard (bit 24)
				mostSignificantByte &= 0b00000001; // mask bit 24 which is stallGuard status, see datasheet p. 52
				int stallDetect = mostSignificantByte;
				if (stallDetect == FLAG_ACTIVE && data[3] == 0 && data[2] == 0) // 1 = Motor stall detected, 0 = no
																				// stall
				// byte 2 and 3 indicate SG_RESULT
				// The stall detection compares SG_RESULT
				// to 0 in order to detect a stall.
				{
					ms.setStallGuardStatus(true);
				} else // 0 = no stall
				{
					ms.setStallGuardStatus(false);
				}
				// overtemperature prewarning flag (bit 26)
				mostSignificantByte &= 0b00000100;
				int overtemperaturePrewarningFlag = mostSignificantByte;
				if (overtemperaturePrewarningFlag == FLAG_ACTIVE) {
					ms.setOverTemperaturePrewarningStatus(true);
				} else {
					ms.setOverTemperaturePrewarningStatus(false);
				}

				// overtemperature flag (bit 25)
				mostSignificantByte &= 0b00000010;
				int overtemperatureFlag = mostSignificantByte;
				if (overtemperatureFlag == FLAG_ACTIVE) {
					ms.setOverTemperatureStatus(true);
				} else {
					ms.setOverTemperatureStatus(false);
				}

				// open load indicator phase A (bit 29)
				mostSignificantByte &= 0b00100000;
				int openLoadIndicatorPhaseA = mostSignificantByte;

				if (openLoadIndicatorPhaseA == FLAG_ACTIVE) {
					ms.setOpenLoadIndicatorStatus("Phase A", true);
				} else {
					ms.setOpenLoadIndicatorStatus("Phase A", false);
				}

				// open load indicator phase B (bit 30)
				mostSignificantByte &= 0b01000000;
				int openLoadIndicatorPhaseB = mostSignificantByte;

				if (openLoadIndicatorPhaseB == FLAG_ACTIVE) {
					ms.setOpenLoadIndicatorStatus("Phase B", true);
				} else {
					ms.setOpenLoadIndicatorStatus("Phase B", false);
				}

				/*
				 * NOTE! The driver becomes disabled. The flags stay active, until the driver is
				 * disabled by software (TOFF=0)
				 */
				mostSignificantByte &= 0b00001000; // bit 27 for S.G.I. on phase A
				int shortToGroundIndicatorPhaseA = mostSignificantByte;

				if (shortToGroundIndicatorPhaseA == FLAG_ACTIVE) {
					ms.setShortToGroundIndicatorStatus("Phase A", true);
				} else {
					ms.setShortToGroundIndicatorStatus("Phase A", false);
				}

				/*
				 * NOTE! The driver becomes disabled. The flags stay active, until the driver is
				 * disabled by software (TOFF=0)
				 */
				mostSignificantByte &= 0b00010000; // bit 28 for S.G.I. on phase B
				int shortToGroundIndicatorPhaseB = mostSignificantByte;

				if (shortToGroundIndicatorPhaseB == FLAG_ACTIVE) {
					ms.setShortToGroundIndicatorStatus("Phase B", true);
				} else {
					ms.setShortToGroundIndicatorStatus("Phase B", false);
				}
			}
			break;
		default:
			break;
		}
		return ms;
	}

	/**
	 * Incoming data packages need to be analyzed in order to extract the relevant
	 * information. At first, as a safety check, the reply package is examined for
	 * any errors. Analysis of the package is done based on the register address.
	 * This is a way to know what kind of request triggered the reply.
	 * 
	 * @param status       Inquired status
	 * @param replyPackage Package of data bytes sent back from the TMC
	 * @return current status of the motor (i.e. stallguard, other errors ...)
	 *         wrapped in a class object
	 * @throws DataCorruptException
	 * @throws CommunicationException
	 */
	MotorStatus analyzeReplyPackage(int status, byte[] replyPackage)
			throws DataCorruptException, CommunicationException {
		MotorStatus motorStatus = new MotorStatus();

		if (replyPackage == null) {
			throw new DataCorruptException("Data is null");
		}

		if (replyPackage.length == 0) { // Why? Because power connector could be jacked out, hence no TMC response
			throw new CommunicationException("Communication error");
		}

		if (replyPackage.length != 8) { // maybe due to sync error?
			throw new DataCorruptException("Data is incomplete");
		}

		String firstByte = Integer.toHexString(Byte.toUnsignedInt(replyPackage[0])).toUpperCase();
		String secondByte = Integer.toHexString(Byte.toUnsignedInt(replyPackage[1])).toUpperCase();

		if (!firstByte.equals("5") || !secondByte.equals("FF")) {
			throw new DataCorruptException("Data is corrupt: sync + slave address");
		}

		byte replyCRC = CRCgenerator.runCRCcheck(replyPackage);

		if (replyCRC != replyPackage[7]) {
			throw new DataCorruptException("Data is corrupt: no CRC match");
		}

		// everything is ok ...
		byte regAddr = replyPackage[2]; // third byte contains register address
		motorStatus = analyzeReplyData(status, regAddr, replyPackage);
		motorStatus.setDataCorruptStatus(false);

		return motorStatus;
	}
}
