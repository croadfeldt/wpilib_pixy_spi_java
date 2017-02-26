package org.usfirst.frc.team3840.robot.subsystems;

import edu.wpi.first.wpilibj.command.Subsystem;
import edu.wpi.first.wpilibj.SPI;
import edu.wpi.first.wpilibj.SPI.Port;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PixySPI {
	PixyPacket values = null;
	SPI pixy = null;
	Port port = null;
	HashMap<Integer, ArrayList<PixyPacket>> packets = null;
	PixyException pExc = null;
	String print;

	// Variables used for pixy comms, derived from https://github.com/omwah/pixy_rpi
	static final byte PIXY_SYNC_BYTE = 0x5a;
	static final byte PIXY_SYNC_BYTE_DATA = 0x5b;
	static final int PIXY_OUTBUF_SIZE = 6;
	static final int PIXY_MAXIMUM_ARRAYSIZE = 130;
	static final int PIXY_START_WORD = 0xaa55;
	static final int PIXY_START_WORDX = 0x55aa;
	static final int BLOCK_LEN = 5;
	static final int PIXY_SIG_COUNT = 7;
	private ArrayDeque<Byte> outBuf = new ArrayDeque<>(); // Future use for sending commands to Pixy.
	private ArrayList<int[]> blocks = new ArrayList<int[]>();
	private boolean skipStart = false;
	private int debug = 0; // 0 - none, 1 - SmartDashboard, 2 - log to console/file 

	private static final Logger logger =
			Logger.getLogger(PixySPI.class.getName());

	long getWord = 0;
	long getStart = 0;
	long getBlock = 0;
	long readPackets = 0;

	public PixySPI(SPI argPixy, HashMap<Integer, ArrayList<PixyPacket>> argPixyPacket, PixyException argPixyException){
		pixy = argPixy;
		packets = argPixyPacket;
		pExc = argPixyException;

		// Set some SPI parameters.
		pixy.setMSBFirst();
		pixy.setChipSelectActiveLow();
		pixy.setClockRate(1000);
		pixy.setSampleDataOnFalling();
		pixy.setClockActiveLow();
	}

	//This method gathers data, then parses that data, and assigns the ints to global variables
	public int readPackets() throws PixyException { //The signature should be which number object in 
		if(debug >= 1) {SmartDashboard.putNumber("readPackets: count: ", readPackets++);}

		// Uncomment this to see just the raw output from the Pixy. You will need to restart the robot code
		// to kill this.
		//rawComms();

		int numBlocks = getBlocks(1000);

		// Clear out and initialize ArrayList for PixyPackets.
		packets.clear();

		for(int i=1; i<=PIXY_SIG_COUNT; i++) {
			packets.put(i, new ArrayList<PixyPacket>());
		}

		// Put the found blocks into the correct spot in the return Hashmap<ArrayList<int[]>>.
		if(numBlocks > 0) {
			if(debug >= 2) {logger.log(Level.INFO, "Pixy readPackets: blocks detected: {0}", Integer.toString(numBlocks));}
			if(debug >= 1) {SmartDashboard.putString("Pixy readPackets: blocks detected: ", Integer.toString(numBlocks));}

			for(int i=0; i < numBlocks; i++) {
				// Create the PixyPacket for the current blocks.
				PixyPacket packet = new PixyPacket();
				int signature = blocks.get(i)[0];
				packet.X = blocks.get(i)[1];
				packet.Y = blocks.get(i)[2];
				packet.Width = blocks.get(i)[3];
				packet.Height = blocks.get(i)[4];

				if(debug >= 1) {SmartDashboard.putString("Pixy readPackets: Signature: " + Integer.toString(signature), Integer.toString(signature));}
				if(debug >= 1) {SmartDashboard.putString("Pixy readPackets: " + Integer.toString(signature) + ": X: ", Integer.toString(packet.X));}
				if(debug >= 1) {SmartDashboard.putString("Pixy readPackets: " + Integer.toString(signature) + ": Y: ", Integer.toString(packet.Y));}
				if(debug >= 1) {SmartDashboard.putString("Pixy readPackets: " + Integer.toString(signature) + ": Width: ", Integer.toString(packet.Width));}
				if(debug >= 1) {SmartDashboard.putString("Pixy readPackets: " + Integer.toString(signature) + ": Height: ", Integer.toString(packet.Height));}
				if(debug >= 2) {logger.log(Level.INFO, "Pixy readPackets: Signature: " + Integer.toString(signature), Integer.toString(signature));}
				if(debug >= 2) {logger.log(Level.INFO, "Pixy readPackets: " + Integer.toString(signature) + ": X: ", Integer.toString(packet.X));}
				if(debug >= 2) {logger.log(Level.INFO, "Pixy readPackets: " + Integer.toString(signature) + ": Y: ", Integer.toString(packet.Y));}
				if(debug >= 2) {logger.log(Level.INFO, "Pixy readPackets: " + Integer.toString(signature) + ": Width: ", Integer.toString(packet.Width));}
				if(debug >= 2) {logger.log(Level.INFO, "Pixy readPackets: " + Integer.toString(signature) + ": Height: ", Integer.toString(packet.Height));}

				// Add the current PixyPacket to the correct location for the signature.
				packets.get(signature).add(packet);
				if(debug >= 1) {SmartDashboard.putNumber("Pixy readPackets: packets size: ", packets.get(signature).size());}
			}
		}

		return(1);
	}

	private int getBlocks(int maxBlocks) {
		// Clear out blocks array list for reuse.
		blocks.clear();
		long count = 0;

		if(debug >= 1) {SmartDashboard.putNumber("getBlock: count: ", getBlock++);}

		// If we haven't found the start of a block, find it.
		if(! skipStart) {
			// If we can't find the start of a block, drop out.
			if(! getStart()) {
				return(0);
			}
		}
		else {
			// Clear flag that tells us to find the next block as the logic below will loop
			// the appropriate number of times to retrieve a complete block.
			skipStart = false;
		}

		// Loop until we hit the maximum size allowed for blocks, or until we know we have a complete set of blocks.
		while ((blocks.size()) < maxBlocks && (blocks.size() < PIXY_MAXIMUM_ARRAYSIZE)) {
			if(debug >= 1) {SmartDashboard.putNumber("getBlocks: loop count: ", count++);}

			// Since this is our first time in, bytes 2 and 3 are the checksum, grab them and store for future use.
			// NOTE: getWord grabs the entire 16 bits in one shot.
			int checksum = getWord();
			int trialsum = 0;

			// See if the checksum is really the beginning of the next block,
			// in which case return the current set of blocks found and set the flag
			// to skip looking for the beginning of the next block since we already found it.
			if(checksum == PIXY_START_WORD) {
				if(debug >= 2) {logger.log(Level.INFO, "Pixy: getBlocks: {0}", "checksum == PIXY_START_WORD");}
				skipStart = true;
				return(blocks.size());
			}
			// See if we received a empty buffer, if so, assume end of comms for now and return what we have.
			else if (checksum == 0) {
				if(debug >= 2) {logger.log(Level.INFO, "Pixy: getBlocks: {0}", "checksum == 0");}
				return(blocks.size());
			}

			// Start building the block which will be stored in the overall set of blocks.
			// Only need 5 slots since the first 3 slots, the double start blocks and checksum, have been retrieved already.
			int[] block = new int[5];
			for(int i=0; i<BLOCK_LEN; i++) {
				block[i] = getWord();
				trialsum += block[i];
				// intsToHex doesn't work yet. Unable to test and fix at the moment.
				// It will show up in the log and SmartDashboard with no data.
				// That does NOT inherently mean anything is wrong with core code.
				if(debug >= 2) {logger.log(Level.INFO, "Pixy: getBlocks: block: {0}", intsToHex(block));}
				if(debug >= 1) {SmartDashboard.putString("Pixy: getBlocks: block", intsToHex(block));}
			}

			if(debug >= 2) {logger.log(Level.INFO, "Pixy: getBlocks checksum: {0}", Integer.toHexString(checksum));}
			if(debug >= 1) {SmartDashboard.putString("Pixy: getBlocks checksum: ", Integer.toHexString(checksum));}
			if(debug >= 2) {logger.log(Level.INFO, "Pixy: getBlocks trialsum: {0}", Integer.toHexString(trialsum));}
			if(debug >= 1) {SmartDashboard.putString("Pixy: getBlocks trialsum: ", Integer.toHexString(trialsum));}

			// See if we received the data correctly.
			if(checksum == trialsum) {
				// Data has been validated, add the current block of data to the overall blocks buffer.
				blocks.add(block);
				if(debug >= 2) {logger.log(Level.INFO, "Pixy: getBlocks Checksum: {0}", "passed");}
				if(debug >= 1) {SmartDashboard.putString("Pixy: getBlocks Checksum", "passed");}
			}
			else {
				if(debug >= 2) {logger.log(Level.INFO, "Pixy: getBlocks Checksum: {0}", "failed");}
				if(debug >= 1) {SmartDashboard.putString("Pixy: getBlocks Checksum", "failed");}
			}

			// Check the next word from the Pixy to confirm it's the start of the next block.
			// Pixy sends two aa55 words at start of block, this should pull the first one.
			// The top of the loop should pull the other one.
			int w = getWord();

			if(debug >= 1) {SmartDashboard.putString("Pixy: getBlocks: w: ", (Integer.toHexString(w)));}
			if(debug >= 2) {logger.log(Level.INFO, "Pixy: getBlocks: w: ", (Integer.toHexString(w)));}

			if (w != PIXY_START_WORD) {
				if(debug >= 2) {logger.log(Level.INFO, "Pixy: getBlocks: {0}", "w != PIXY_START_WORD");}
				return(blocks.size());
			}
		}

		// Should never get here, but if we happen to get a massive number of blocks
		// and exceed the limit it will happen. In that case something is wrong
		// or you have a super natural Pixy and SPI link.
		return(0);
	}

	// Pixy SPI comm functions derived from https://github.com/omwah/pixy_rpi
	private int getWord() {
		int word = 0x00;
		int ret = -1;
		ByteBuffer writeBuf = ByteBuffer.allocateDirect(2);
		writeBuf.order(ByteOrder.BIG_ENDIAN);
		ByteBuffer readBuf = ByteBuffer.allocateDirect(2);
		readBuf.order(ByteOrder.BIG_ENDIAN);
		String readString = null;
		String writeString = null;

		if(debug >= 1) {SmartDashboard.putNumber("getWord: count: ", getWord++);}

		if (outBuf.size() > 0) {
			writeBuf.put(PIXY_SYNC_BYTE_DATA);
		}
		else {
			writeBuf.put(PIXY_SYNC_BYTE);
		}

		// Flip the writeBuf so it's ready to be read.
		writeBuf.flip();

		// Store the value of the buffer to a string for debugging to the console/log and SmartDashboard.
		if(debug >= 1) {
			writeString = bbToString(writeBuf);
			SmartDashboard.putString("Pixy: getWord: write sync: ", writeString);
		}
		if(debug >= 2) {logger.log(Level.INFO, "Pixy: getWord: write sync: {0}", writeString);}

		// Send the sync / data bit / 0 to get the Pixy to return data appropriately.
		ret = pixy.transaction(writeBuf, readBuf, 2);

		//Store the value of the buffer to a string for debugging to the console/log and SmartDashboard
		if(debug >= 1) {
			readString = bbToString(readBuf);
			SmartDashboard.putString("Pixy: getWord: read sync: ", readString);
		}
		if(debug >= 2) {logger.log(Level.INFO, "Pixy: getWord: read sync: {0}", readString);}

		// Set the position back to 0 in the buffer so we read it from the beginning next time.
		readBuf.rewind();

		// Store the contents of the buffer in a int that will be returned to the caller.
		word = (int) (readBuf.getShort() & 0xffff);

		if(debug >= 1) {SmartDashboard.putString("Pixy: getWord: word: ", Integer.toHexString(word));}

		// Clear the buffers, not needed, but nice to know they are cleaned out.
		writeBuf.clear();
		readBuf.clear();
		return(word);
	}

	// Need to come back to this one, used only for send control data to Pixy.
	private int send(byte data) {
		return(0);
	}

	private boolean getStart() {
		int lastw = 0xff;
		int count=0;

		if(debug >= 1) {SmartDashboard.putNumber("getStart: count: ", getStart++);}

		// Loop until we get a start word from the Pixy.
		while(true) {
			int w = getWord();
			if(debug >= 2) {logger.log(Level.INFO, "Pixy: getStart: w: {0}", Integer.toHexString(w));}
			if(debug >= 1) {
				SmartDashboard.putString("Pixy: getStart: w", Integer.toHexString(w));
				SmartDashboard.putNumber("getStart: loop count: ", count++);
			}
			if ((w == 0x00) && (lastw == 0x00)) {
				// Could delay a bit to give time for next data block, but to get accurate time would tie up cpu.
				// So might as well return and let caller call this getStart again.
				if(debug >= 2) {logger.log(Level.INFO, "Pixy: getStart: {0}", "no pixy data received");}
				if(debug >= 1) {SmartDashboard.putString("Pixy: getStart", "no pixy data received");}
				return(false);
			}
			else if (((int) w == PIXY_START_WORD) && ((int) lastw == PIXY_START_WORD)) {
				if(debug >= 2) {logger.log(Level.INFO, "Pixy: getStart: {0}", "found start");}
				if(debug >= 1) {SmartDashboard.putString("Pixy: getStart", "found start");}
				return(true);
			}

			lastw = w;
		}
	}

	// Some debugging functions and assorted trinkets.
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for ( int j = 0; j < bytes.length; j++ ) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	// This doesn't work yet. At least that's what I recall.
	public static String intsToHex(int[] ints) {
		String hexString = new String();
		for ( int j = 0; j < ints.length; j++ ) {
			hexString.concat(Integer.toHexString(ints[j]));
		}
		return new String(hexString);
	}

	// Call this from readPackets, it's there commented out already.
	// Read the warning there as well.
	// This will dump all data to the console/log and SmartDashboard
	// Set the debug level accordingly, otherwise it's going to be a bit boring
	// watching nothing happen while your RoboRio is screaming into the void.
	public void rawComms() {
		//logger.entering(getClass().getName(), "doIt");
		int ret = -1;
		ByteBuffer buf = ByteBuffer.allocate(2);
		buf.order(ByteOrder.BIG_ENDIAN);
		ByteBuffer writeBuf = ByteBuffer.allocateDirect(2);
		writeBuf.order(ByteOrder.BIG_ENDIAN);
		ByteBuffer readBuf = ByteBuffer.allocateDirect(2);
		readBuf.order(ByteOrder.BIG_ENDIAN);
		String readString = null;
		String writeString = null;

		while (true) {
			writeBuf.put(PIXY_SYNC_BYTE);
			writeBuf.flip();
			writeString = bbToString(writeBuf);
			if(debug >= 2) {logger.log(Level.INFO, "Pixy: rawComms: write sync: {0}", writeString);}
			if(debug >= 1) {SmartDashboard.putString("Pixy: rawComms: write sync: ", writeString);}
			ret = pixy.transaction(writeBuf, readBuf, 2);
			//readBuf.flip();
			readString = bbToString(readBuf);
			if(debug >= 2) {logger.log(Level.INFO, "Pixy: rawComms: read sync: {0}", readString);}
			readBuf.rewind();
			if(debug >= 1) {SmartDashboard.putString("Pixy: rawComms: read sync: ", readString);}
			writeBuf.clear();
			readBuf.clear();
		}
	}

	public String bbToString(ByteBuffer bb) {
		final byte[] b = new byte[bb.remaining()];
		bb.duplicate().get(b);
		bb.rewind();
		return new String(bytesToHex(b));
	}

}

