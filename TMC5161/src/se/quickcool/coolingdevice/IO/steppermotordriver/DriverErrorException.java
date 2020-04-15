package se.quickcool.coolingdevice.IO.steppermotordriver;

/**
 * This class represents errors which cause exceptions that relate to motor
 * driver errors, for instance if the motor has stalled, which are enumerated in
 * the TMC status register DRV_STATUS.
 * 
 * @author Danial Mahmoud <<i>danial.mahmoud@quickcool.se</i>>
 * @version 1.0
 */
public class DriverErrorException extends Exception {
	private String driverErrorMsg;

	/**
	 * Constructor creates an instance of this class which takes as an input
	 * parameter a clarifying explanation of the error that has occurred.
	 * 
	 * @param driverErrorMsg Description of the error message
	 */
	public DriverErrorException(String driverErrorMsg) {
		this.driverErrorMsg = driverErrorMsg;
	}

	/**
	 * Return a description of the error that this type of exception represents.
	 * 
	 * @return clarifying description of the driver error
	 */
	public String getDriverErrorMessage() {
		return driverErrorMsg;
	}

	public String getLocalizedMessage() {
		return driverErrorMsg;
	}

}
