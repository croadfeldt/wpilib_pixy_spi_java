package org.usfirst.frc.team3840.robot.subsystems;

import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.I2C;
import edu.wpi.first.wpilibj.I2C.Port;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;


public class PixyI2C {
	PixyPacket values;
	I2C pixy;
	Port port = Port.kOnboard;
	PixyPacket[] packets;
	PixyException pExc;
	String print;
	public PixyI2C() {
//		pixy = new I2C(port, 0x54);
//		packets = new PixyPacket[7];
//		pExc = new PixyException(print);
//		values = new PixyPacket();
	}
	public PixyI2C(I2C argPixy, PixyPacket[] argPixyPacket, PixyException argPixyException, PixyPacket argValues){
		pixy = argPixy;
		packets = argPixyPacket;
		pExc = argPixyException;
		values = argValues;
	}
	//This method parses raw data from the pixy into readable integers
	public int cvt(byte upper, byte lower) {
		return (((int)upper & 0xff) << 8) | ((int)lower & 0xff);
	}
	/*
	public void pixyReset(){
		pixy.reset();
	}
	*/
	//This method gathers data, then parses that data, and assigns the ints to global variables
	public PixyPacket readPacket(int Signature) throws PixyException { //The signature should be which number object in 
		int Checksum;												   //pixymon you are trying to get data for
		int Sig;
		byte[] rawData = new byte[32];
		SmartDashboard.putString("rawData", rawData[0] + " " + rawData[1] + " " + rawData[15] + " " + rawData[31]);		
		try{
			pixy.readOnly(rawData, 32);	
		} catch (RuntimeException e){
			SmartDashboard.putString("Pixy RuntimeException", "Error");
		}
		if(rawData.length < 32){
			System.out.println("byte array length is broken");
			return null;
		}
		for (int i = 0; i <= 16; i++) {
			int syncWord = cvt(rawData[i+1], rawData[i+0]); //Parse first 2 bytes
			if (syncWord == 0xaa55) { //Check is first 2 bytes equal a "sync word", which indicates the start of a packet of valid data
				syncWord = cvt(rawData[i+3], rawData[i+2]); //Parse the next 2 bytes
				if (syncWord != 0xaa55){ //Shifts everything in the case that one syncword is sent
					i -= 2;
				}
				//This next block parses the rest of the data
				Checksum = cvt(rawData[i+5], rawData[i+4]);
				Sig = cvt(rawData[i+7], rawData[i+6]);
				if(Sig <= 0 || Sig > packets.length){
					break;
				}
				
				packets[Sig - 1] = new PixyPacket();
				packets[Sig - 1].X = cvt(rawData[i+9], rawData[i+8]);
				packets[Sig - 1].Y = cvt(rawData[i+11], rawData[i+10]);
				packets[Sig - 1].Width = cvt(rawData[i+13], rawData[i+12]);
				packets[Sig - 1].Height = cvt(rawData[i+15], rawData[i+14]);
				//Checks whether the data is valid using the checksum *This if block should never be entered*
				if (Checksum != Sig + packets[Sig - 1].X + packets[Sig - 1].Y + packets[Sig - 1].Width + packets[Sig - 1].Height) {
					packets[Sig - 1] = null;
					throw pExc;
				}
				break;
			}
			else
				SmartDashboard.putNumber("syncword: ", syncWord);
		}
		//Assigns our packet to a temp packet, then deletes data so that we dont return old data
		PixyPacket pkt = packets[Signature - 1];
		packets[Signature - 1] = null;
		return pkt;
	}
	
	public int getX(){
		return values.X;
	}
	
	public int getY(){
		return values.Y;
	}
	
	public int getWidth(){
		return values.Width;
	}
	
	public int getHeight(){
		return values.Height;
	}
	
	public int getArea(){
		return getWidth() * getHeight();
	}
	
}

