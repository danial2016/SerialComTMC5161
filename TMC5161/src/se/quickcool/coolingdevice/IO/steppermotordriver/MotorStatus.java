package se.quickcool.coolingdevice.IO.steppermotordriver;

/**
 * This class contains methods that check and set the motor status based on
 * analysis of incoming reply packages. This allows user programs to monitor its
 * condition and operation to check if things are in order. Otherwise, the
 * appropriate error status is set allowing other parts of the program to make
 * decisions accordingly.
 * 
 * @author Danial Mahmoud <<i>danial.mahmoud@quickcool.se</i>>
 * @version 1.0
 */
class MotorStatus {
	private int actualVelocityStatus;
	private boolean dataCorruptStatus;
	private boolean stallGuardStatus;
	private boolean overTemperatureStatus;
	private boolean openLoadIndicatorStatusPhaseA;
	private boolean openLoadIndicatorStatusPhaseB;
	private boolean shortToGroundIndicatorStatusPhaseA;
	private boolean shortToGroundIndicatorStatusPhaseB;
	private boolean overTemperaturePrewarningStatus;

	/**
	 * Returns the instantaneous value of the actual speed of the motor in
	 * <i>microsteps per revolution</i>.
	 * 
	 * @return current actual motor speed
	 */
	int getActualVelocityStatus() {
		return actualVelocityStatus;
	}

	/**
	 * Set the value of the current motor speed to the value received from the TMC.
	 * <p>
	 * <b>Note:</b> This function does not set a new speed for the motor, rather
	 * just a status value that can be rechecked when a user program inquires
	 * velocity status.
	 * </p>
	 * 
	 * @param actualVelocityStatus actual velocity of motor in units of microsteps
	 *                             per revolution
	 */
	void setActualVelocityStatus(int actualVelocityStatus) {
		this.actualVelocityStatus = actualVelocityStatus;
	}

	/**
	 * Corrupt data means data that is incomplete or has distorted bytes. This
	 * function returns the result of an data-integrity-check of a reply package.
	 * 
	 * @return whether the data is corrupt or not
	 */
	boolean getDataCorruptStatus() {
		return dataCorruptStatus;
	}

	/**
	 * Set the status of an incoming data package from the TMC to whether if it is
	 * corrupt or not.
	 * 
	 * @param dataCorruptStatus boolean value indicating corruption status of a
	 *                          reply package
	 */
	void setDataCorruptStatus(boolean dataCorruptStatus) {
		this.dataCorruptStatus = dataCorruptStatus;
	}

	/**
	 * The status of the stall guard indicates whether the motor has stalled due any
	 * kind of stoppage. This function returns if the motor has stalled or not. The
	 * stall guard is a function installed in the TMC that checks automatically for
	 * any motor stalls. Stalling means that the motor rotational movement is
	 * impeded which prevents it from rotating any further.
	 * 
	 * @return boolean value indicating if the motor has stalled or not
	 */
	boolean getStallGuardStatus() {
		return stallGuardStatus;
	}

	/**
	 * Set the status of the stall guard to a value that indicates whether the motor
	 * has stalled or not. The stall guard is a function installed in the TMC that
	 * checks automatically for any motor stalls. Stalling means that the motor
	 * rotational movement is impeded which prevents it from further rotation.
	 * 
	 * @param stallGuardStatus
	 */
	void setStallGuardStatus(boolean stallGuardStatus) {
		this.stallGuardStatus = stallGuardStatus;
	}

	/**
	 * Returns whether the motor device has crossed the warning threshold for
	 * overtemperature.
	 * <p>
	 * <b>Note:</b> Prewarning occurs at 120°C.
	 * </p>
	 * 
	 * @return value in register that indicates if the motor temperature has
	 *         exceeded the prewarning threshold
	 */
	boolean getOverTemperaturePrewarningStatus() {
		return overTemperaturePrewarningStatus;
	}

	/**
	 * Set the overtemperature prewarning status according to whether the motor
	 * temperature has exceeded the prewarning threshold or not.
	 * 
	 * @param overTemperaturePrewarningStatus
	 */
	void setOverTemperaturePrewarningStatus(boolean overTemperaturePrewarningStatus) {
		this.overTemperaturePrewarningStatus = overTemperaturePrewarningStatus;
	}

	/**
	 * Return if the current motor temperature has exceeded the maximum allowed
	 * temperature threshold.
	 * <p>
	 * <b>Note:</b> Prewarning occurs at 120°C, after that a selectable 136 °C/ 143
	 * °C/ 150 °C thermal shutdown.
	 * </p>
	 * 
	 * @return whether the motor has crossed overtemperature threshold
	 */
	boolean getOverTemperatureStatus() {
		return overTemperatureStatus;
	}

	/**
	 * Set if the current motor temperature has exceeded the maximum allowed
	 * temperature threshold.
	 * <p>
	 * <b>Note:</b> Prewarning occurs at 120°C, after that a selectable 136 °C/ 143
	 * °C/ 150 °C thermal shutdown.
	 * </p>
	 * 
	 * @param overTemperatureStatus
	 */
	void setOverTemperatureStatus(boolean overTemperatureStatus) {
		this.overTemperatureStatus = overTemperatureStatus;
	}

	/**
	 * The TMC5161 detects open load conditions by checking, if it can reach the
	 * desired motor coil current. Interrupted cables are a common cause for systems
	 * failing, e.g. when connectors are not firmly plugged.
	 * 
	 * @param phase representation of the motor coil to be checked for any open load
	 * @return open load indication (i.e. interrupted coil connection)
	 */
	boolean getOpenLoadIndicatorStatus(String phase) {
		switch (phase) {
		case "Phase A":
			return openLoadIndicatorStatusPhaseA;
		case "Phase B":
			return openLoadIndicatorStatusPhaseB;
		default:
			break;
		}
		return false;
	}

	/**
	 * Set whether any open load has occurred on the motor coils.
	 * <p>
	 * <b>Note:</b> Open load means the current on a motor coil has been
	 * interrupted, for instance through a connector being poorly plugged in.
	 * </p>
	 * 
	 * @param phase                   string representation of the motor coil to be
	 *                                checked for any open load
	 * @param openLoadIndicatorStatus actual status of the TMC:s open load
	 *                                monitoring
	 */
	void setOpenLoadIndicatorStatus(String phase, boolean openLoadIndicatorStatus) {
		if (phase.equals("Phase A")) {
			this.openLoadIndicatorStatusPhaseA = openLoadIndicatorStatus;
		} else if (phase.equals("Phase B")) {
			this.openLoadIndicatorStatusPhaseB = openLoadIndicatorStatus;
		} else {
			System.out.println("Wrong input - the format " + phase + " is incorrect");
		}
	}

	/**
	 * Return an indication of whether a motor coil has been shorted to ground, e.g.
	 * through a badly connected cable. The TMC examines this by measuring the
	 * current on the coil reaches the desired value.
	 * 
	 * @param phase string representation of the motor coil to be checked for
	 *              shortage
	 * @return status for short to ground detection
	 */
	boolean getShortToGroundIndicatorStatus(String phase) {
		switch (phase) {
		case "Phase A":
			return shortToGroundIndicatorStatusPhaseA;
		case "Phase B":
			return shortToGroundIndicatorStatusPhaseB;
		default:
			break;
		}
		return false;
	}

	/**
	 * Set an indication of whether a motor coil has been shorted to ground, e.g.
	 * through a badly connected cable. The TMC examines this by measuring the
	 * current on the coil.
	 * 
	 * @param phase                        string representation of the motor coil
	 *                                     to be checked for shortage
	 * @param shortToGroundIndicatorStatus actual status of the TMC:s short to
	 *                                     ground monitoring
	 */
	void setShortToGroundIndicatorStatus(String phase, boolean shortToGroundIndicatorStatus) {
		if (phase.equals("Phase A")) {
			this.shortToGroundIndicatorStatusPhaseA = shortToGroundIndicatorStatus;
		} else if (phase.equals("Phase B")) {
			this.shortToGroundIndicatorStatusPhaseB = shortToGroundIndicatorStatus;
		} else {
			System.out.println("Wrong input - the format " + phase + " is incorrect");
		}
	}

}
