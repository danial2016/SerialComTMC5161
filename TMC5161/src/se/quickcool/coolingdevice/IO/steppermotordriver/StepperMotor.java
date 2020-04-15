package se.quickcool.coolingdevice.IO.steppermotordriver;

/**
 * This class allows other user programs to interact with the Stepper motor.
 * Interaction could mean setting velocity, checking its condition and operation
 * status and so on. An external user class only has access to this class, while
 * the rest of the system does the heavy lifting behind the scenes.
 * 
 * @author Danial Mahmoud <<i>danial.mahmoud@quickcool.se</i>>
 * @version 1.0
 */
public class StepperMotor {
	private StepperMotorControl smc;

	// Shared list of status parameters
	public static final int ACTUAL_VELOCITY_STATUS = 0, STALLGUARD_STATUS = 1, OVERTEMPERATURE_PREWARNING_STATUS = 2,
			OVERTEMPERATURE_STATUS = 3, OPEN_LOAD_INDICATOR_PHASE_A = 4, OPEN_LOAD_INDICATOR_PHASE_B = 5,
			SHORT_TO_GROUND_INDICATOR_PHASE_A = 6, SHORT_TO_GROUND_INDICATOR_PHASE_B = 7, DRIVER_ERROR_STATUS = 8;

	/**
	 * Create an instance of an object representing the stepper motor. Constructor
	 * initializes the control class for the stepper motor.
	 * 
	 * @throws CommunicationException
	 */
	public StepperMotor() {
		this.smc = new StepperMotorControl();
	}

	/**
	 * Initializes and starts the stepper motor with the input parameter as initial
	 * RPS speed.
	 * <p>
	 * 
	 * @param rps Velocity of the motor given in <em>Rotations per Second </em>(RPS)
	 * @return true if the speed is within a valid range, otherwise false
	 *         </p>
	 * @throws CommunicationException
	 * @throws DriverErrorException
	 */
	public void startStepperMotor(double rps) throws CommunicationException, DriverErrorException {
		smc.initStepperMotor(rps);
		smc.rotateToTargetPosition(0x00099000); // starts stepper motor
	}

	/**
	 * Resets the position counter, i.e. sets XTARGET to zero.
	 */
	public void resetPositionCounter() {
		smc.resetPositionCounter();
	}

	/**
	 * Check status registers of the driver board and throw an exception if an error
	 * has occurred. This method can also check the velocity of the motor.
	 * <p>
	 * <b>Note:</b> Exceptions are caught but not handled. User must handle the
	 * exceptions and make the decision based on them.
	 * </p>
	 * 
	 * @param status status which is inquired such as current motor velocity
	 * @throws DataCorruptException
	 * @throws DriverErrorException
	 */
	public void checkMotorStatus() throws CommunicationException, DataCorruptException, DriverErrorException {
		smc.checkMotorStatus();
	}

	/**
	 * Resets the motor by power cycling VCC_IO. This completely resets the chip.
	 * This function is to be used as a way to get out of exceptions.
	 * 
	 * @throws DriverErrorException
	 * @throws CommunicationException
	 * 
	 */
	public void resetMotor(double rps) {
		smc.powerCycle(); // cycles VCC_IO to completely reset the chip
		smc.TMC5161Configuration(rps);
		smc.resetPositionCounter(); // TODO necessary? Does chip reset also reset position counter (i.e. sets
									// XTARGET to zero)?
		smc.rotateToTargetPosition(0x00099000);
	}

	/**
	 * Enables the possibility to throw an exception as a result of motor stall.
	 * Gives the user the option to enable or disable stall detection monitoring.
	 * 
	 * @param enableStallguard
	 */
	public void enableStallguardException(boolean enableStallguardException) {
		smc.enableStallguardException(enableStallguardException);
	}

	/**
	 * Enables the possibility to throw an exception as a result of motor exceeding
	 * allowable velocity limits. Gives the user the option to enable or disable
	 * velocity monitoring.
	 * 
	 * @param enableVelocity
	 */
	public void enableVelocityException(boolean enableVelocityException) {
		smc.enableVelocityException(enableVelocityException);
	}

	/**
	 * Sets a new target velocity which the stepper motor will rotate at if the
	 * value does not violate the permissible range.
	 * 
	 * @param rps new target velocity
	 * @throws DriverErrorException exception thrown if value is invalid
	 */
	public void setNewTargetVelocity(double rps) throws DriverErrorException {
		smc.setNewTargetVelocity(rps);
	}
}
