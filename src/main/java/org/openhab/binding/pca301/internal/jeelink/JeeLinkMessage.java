/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.pca301.internal.jeelink;

import java.text.ParseException;

/**
 * Represents a message from or to a PCA301 device.
 * 
 * 
 * @author ribbeck
 * @since 1.7.2
 * @see <a href="http://forum.fhem.de/index.php?topic=11648.0">Protocol Analysis (german)</a>
 */
public class JeeLinkMessage implements Comparable<JeeLinkMessage> {
	
	public final static int CMD_VALUES		= 0x04;
	public final static int CMD_STATE		= 0x05;
	public final static int CMD_PAIRING		= 0x11;
	
	/** In combination with {@link #CMD_VALUES} triggers a refresh */
	public final static int PARAM_NONE		= 0x00;
	/** In combination with {@link #CMD_VALUES} resets values. */
	public final static int PARAM_RESET		= 0x01;
	/** In combination with {@link #CMD_STATE} changes the switching state to off. */
	public final static int PARAM_OFF		= 0x00;
	/** In combination with {@link #CMD_STATE} changes the switching state to on. */
	public final static int PARAM_ON		= 0x01;
	
	private final static String MSG_FORMAT = "%d,%d,%d,%d,%d,%d,255,255,255,255s";
	
	int address;
	int channel;
	
	int cmd;
	int param;
	
	int power;
	int consumption;
	
	/**
	 * Constructor to initialize the message.
	 * @param address PCA301 device address
	 * @param channel communication channel
	 * @param cmd command either {@link JeeLinkMessage#CMD_VALUES} or {@link JeeLinkMessage#CMD_STATE}
	 * @param param parameter which is suitable to the command
	 */
	public JeeLinkMessage(int address, int channel, int cmd, int param) {
		this.address = address;
		this.channel = channel;
		this.cmd = cmd;
		this.param = param;
	}
	
	/** Returns the PCA301 device address. */
	public int getAddress() {
		return address;
	}

	/** Returns the communication channel. */
	public int getChannel() {
		return channel;
	}

	/** Returns the message command. */
	public int getCommand() {
		return cmd;
	}

	/** Returns the command parameter. */
	public int getParameter() {
		return param;
	}

	/** Returns the current power in W. */
	public double getPower() {
		return power / 10.0;
	}

	/** Returns the total power consumption in kWh. */
	public double getConsumption() {
		return consumption / 100.0;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		
		if (obj instanceof JeeLinkMessage) {
			return compareTo((JeeLinkMessage)obj) == 0;
		}
		
		return false;
	}
	
	@Override
	public int compareTo(JeeLinkMessage o) {
		if (o == null) {
			return 1;
		}
		if (address != o.address) {
			return (address - o.address);
		}
		if (channel != o.channel) {
			return (channel - o.channel);
		}
		if (cmd != o.cmd) {
			return (cmd - o.cmd);
		}
		if (param != o.param) {
			return (param - o.param);
		}
		if (power != o.power) {
			return (power - o.power);
		}
		if (consumption != o.consumption) {
			return (consumption - o.consumption);
		}
				
		return 0;
	}

	/** 
	 * Returns the message as string which can be send to serial port.
	 * @see #MSG_FORMAT
	 */
	public String toSerialString() {
		
		int[] addressBytes = new int[3];
		addressBytes[0] = (address >> 16) & 0xff;
		addressBytes[1] = (address >>  8) & 0xff;
		addressBytes[2] = (address      ) & 0xff;
		
		return String.format(MSG_FORMAT, channel, cmd, addressBytes[0], addressBytes[1], addressBytes[2], param);
	}

	/**
	 * Parses a string received from from serial port and creates a message.<br>
	 * The message must be in format:<br>
	 * {@code <ch> <cmd> <addr[0]> <addr[1]> <addr[2]> <param> <power[0]> <power[1]> <cons[0]> <cons[1]>}
	 * @param data string from serial port
	 * @return the message as object
	 * @throws ParseException The data was not in correct format
	 */
	public static JeeLinkMessage parseSerialString(String data) throws ParseException {
		
		final String[] parts = data.trim().split(" ");
		
		if (parts.length != 10) {
			throw new ParseException(data, 0);
		}
		
		int index = 0;
		
		int channel = Integer.parseInt(parts[index++]);
		int cmd = Integer.parseInt(parts[index++]);
		
		int[] addressBytes = new int[3];
		addressBytes[0] = Integer.parseInt(parts[index++]);
		addressBytes[1] = Integer.parseInt(parts[index++]);
		addressBytes[2] = Integer.parseInt(parts[index++]);
		int address = getValue(addressBytes);
		
		int param = Integer.parseInt(parts[index++]);
		
		final JeeLinkMessage msg = new JeeLinkMessage(address, channel, cmd, param);
		
		if (cmd == CMD_VALUES) {
			
			int[] powerBytes = new int[2];
			powerBytes[0] = Integer.parseInt(parts[index++]);
			powerBytes[1] = Integer.parseInt(parts[index++]);
			msg.power = (short)getValue(powerBytes);
			
			int[] powerConsumption = new int[2];
			powerConsumption[0] = Integer.parseInt(parts[index++]);
			powerConsumption[1] = Integer.parseInt(parts[index++]);
			msg.consumption = (short)getValue(powerConsumption);
		}
		
		return msg;
	}
	
	/** Converts a byte array into an integer (big endian). **/
	private static int getValue(int[] bytes) {
		
		if (bytes.length > (Integer.SIZE / Byte.SIZE)) {
			throw new IllegalArgumentException("Too much byte values");
		}
		
		int result = 0;
		for (int i = 0; i < bytes.length; i++) {
			result |= (bytes[i] & 0xff) << (8 * (bytes.length - i - 1)); 
		}
		
		return result;
	}
}
