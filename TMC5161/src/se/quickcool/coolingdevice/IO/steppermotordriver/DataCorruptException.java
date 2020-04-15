package se.quickcool.coolingdevice.IO.steppermotordriver;

/**
 * This class represents errors which cause exceptions that relate to any kind
 * of corruption in incoming databytes from the TMC motor driver retrieved at
 * the serial port.
 * 
 * @author Danial Mahmoud <<i>danial.mahmoud@quickcool.se</i>>
 * @version 1.0
 */

public class DataCorruptException extends Exception {
	private String corruptDataMessage;

	/**
	 * Constructor creates an instance of this class which takes as an input
	 * parameter a clarifying explanation of the error that has occurred.
	 * 
	 * @param corruptDataMessage Description of the error message
	 */
	public DataCorruptException(String corruptDataMessage) {
		this.corruptDataMessage = corruptDataMessage;
	}

	/**
	 * Return a description of the error that this type of exception represents.
	 * 
	 * @return clarifying description of the corrupt data error
	 */
	public String getCorruptDataMessage() {
		return corruptDataMessage;
	}

	@Override
	public String getLocalizedMessage() {
		return corruptDataMessage;
	}

}
