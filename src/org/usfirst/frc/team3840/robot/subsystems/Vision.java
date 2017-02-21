package org.usfirst.frc.team3840.robot.subsystems;

import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.I2C;
import edu.wpi.first.wpilibj.I2C.Port;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;


public class Vision extends Subsystem {

	
	public PixyI2C pixy1;
	
	Port port = Port.kOnboard;
	String print;
	public PixyPacket[] packet1 = new PixyPacket[7];
	public PixyPacket[] packet2 = new PixyPacket[7];
	
	public Vision(){
		pixy1 = new PixyI2C(new I2C(port, 0x54), packet1, new PixyException(print), new PixyPacket());
	}

	
	public void initDefaultCommand() {
		// Set the default command for a subsystem here.
		// setDefaultCommand(new MySpecialCommand());
	}
	
	public void testPixy1(){
		for(int i = 0; i < packet1.length; i++)
			packet1[i] = null;
		SmartDashboard.putString("Pixy1 hello", "working");
		for(int i = 1; i < 8; i++) {   		
			try {
				packet1[i - 1] = pixy1.readPacket(i);
			} catch (PixyException e) {
				SmartDashboard.putString("Pixy1 Error: " + i, "exception");
			}
			if(packet1[i - 1] == null){
				SmartDashboard.putString("Pixy1 Error: " + i, "True");
				continue;
			}
			SmartDashboard.putNumber("Pixy1 X Value: " + i, packet1[i - 1].X);
			SmartDashboard.putNumber("Pixy1 Y Value: " + i, packet1[i - 1].Y);
			SmartDashboard.putNumber("Pixy1 Width Value: " + i, packet1[i - 1].Width);
			SmartDashboard.putNumber("Pixy1 Height Value: " + i, packet1[i - 1].Height);
			SmartDashboard.putString("Pixy1 Error: " + i, "False");
		}
	}

}

