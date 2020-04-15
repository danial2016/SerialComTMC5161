package se.quickcool.coolingdevice.IO.steppermotordriver;

/**
 * This class represents errors which cause exceptions that relate to
 * communication across the serial interface with the TMC motor driver.
 * 
 * @author Danial Mahmoud <<i>danial.mahmoud@quickcool.se</i>>
 * @version 1.0
 */
public class CommunicationException extends Exception {
	private String communicationErrorDescription;

	/**
	 * Constructor creates an instance of this class which takes as an input
	 * parameter a clarifying explanation of the error that has occurred.
	 * 
	 * @param communicationErrorDescription Description of the error message
	 */
	public CommunicationException(String communicationErrorDescription) {
		this.communicationErrorDescription = communicationErrorDescription;
	}

	/**
	 * Return a description of the error that this type of exception represents.
	 * 
	 * @return clarifying description of the communication error
	 */
	public String getCommunicationErrorDescription() {
		return communicationErrorDescription;
	}

	/**
	 * To be used due to multi-try-catch
	 */
	@Override
	public String getLocalizedMessage() {
		return communicationErrorDescription;
	}
}
