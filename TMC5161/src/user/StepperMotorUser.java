package user;

import se.quickcool.coolingdevice.IO.steppermotordriver.CommunicationException;
import se.quickcool.coolingdevice.IO.steppermotordriver.DataCorruptException;
import se.quickcool.coolingdevice.IO.steppermotordriver.DriverErrorException;
import se.quickcool.coolingdevice.IO.steppermotordriver.StepperMotor;

/*
 * Personal memory notes: The TMC5161-EVAL from Trinamics is running the Stepper
 * Motor, its supply voltage is 24 V. Our Guest OS (Ubuntu) has access to our
 * Host OS (Windows) serial port (in this case COM2). We are hence not using any
 * USB port, so DO NOT add the USB port that the FTDI cable is assigned to.
 * Luckily, the USB filter will not get captured automatically. All YOU NEED TO
 * DO, is to hook in the FTDI-RS485 cable that is connected to the Trinamics-
 * board and then just make sure that we have enabled serial port 2 (inside
 * settings in the Virtual Box) and then run this miserable program. Even a
 * monkey wouldn't get that wrong. Thank you.
 */

public class StepperMotorUser {

	public static void main(String[] args) {
		StepperMotor stepperMotor = new StepperMotor();

		try {
			stepperMotor.startStepperMotor(2.5);
			stepperMotor.enableStallguardException(true);
		} catch (CommunicationException | DriverErrorException e) {
			System.out.println("Exception thrown: " + e.getLocalizedMessage());
			if (e.getLocalizedMessage().equals("Stall detected!")
					|| e.getLocalizedMessage().equals("Overtemperature pre-warning threshold is exceeded")) {
			} else {
				System.out.println("Resetting motor ... \n");
				stepperMotor.resetMotor(2.5);
				stepperMotor.enableStallguardException(true);
			}
		}

		while (true) {
			long start = System.currentTimeMillis();
			try {
				stepperMotor.checkMotorStatus();
			} catch (CommunicationException | DataCorruptException | DriverErrorException e) {
				System.out.println("Exception thrown: " + e.getLocalizedMessage());
				if (e.getLocalizedMessage().equals("Stall detected!")
						|| e.getLocalizedMessage().equals("Overtemperature pre-warning threshold is exceeded")) {
				} else {
					System.out.println("Resetting motor ... \n");
					stepperMotor.resetMotor(2.5);
					stepperMotor.enableStallguardException(true);
				}
			}
			stepperMotor.resetPositionCounter();

			long stop = System.currentTimeMillis();
			System.out.println("Elapsed time: " + (stop - start) / 1000 + " seconds");

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}
}
