/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.pca301.internal.jeelink;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TooManyListenersException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.openhab.binding.pca301.internal.jeelink.JeeLinkFilterSketch.InvalidSketchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class provides access to a JeeLink device with the pcaSerial sketch.
 * 
 * @author ribbeck
 * @since 1.7.2
 */
public class JeeLinkDevice implements SerialPortEventListener {
	
	private final static Logger logger = LoggerFactory.getLogger(JeeLinkDevice.class);
	
	private final static int RETRY_DELAY	= 3; // in seconds
	
	private String port = null;
	private final int retryCount;
	
	private SerialPort serialPort = null;
	private InputStream input = null;
	private OutputStream output = null;
	private BufferedReader reader = null;
	private BufferedWriter writer = null;
	private boolean isOpen = false;
	
	/** Collection of listeners. Access must be synchronized. */
	private final Set<JeeLinkListener> listeners = new HashSet<JeeLinkListener>();
	
	private final List<JeeLinkFilter> filters = new ArrayList<JeeLinkFilter>();
	
	
	/** Mapping of message and pending retry tasks. Access must be synchronized. */
	private final Map<JeeLinkMessage, Future<?>> pendingTasks = new HashMap<JeeLinkMessage, Future<?>>();
	/** Executor to run retry task. Access must be synchronized over {@link #pendingTasks}. */
	private ScheduledExecutorService executor = null;
	
	
	/**
	 * Constructor
	 * @param port Serial port which is used to connect the JeeLink device (e.g. "/dev/ttyUSB0")
	 */
	public JeeLinkDevice(String port, int retryCount) {
		this.port = port;
		this.retryCount = retryCount;
		
		filters.add(new JeeLinkFilterRegex("^OK 24 (.*)$"));
		filters.add(new JeeLinkFilterRegex("^L 24 \\d+ \\d+ : (.*)$"));
		filters.add(new JeeLinkFilterRegex("^R \\d+ : (.*)$"));
		filters.add(new JeeLinkFilterSketch(10, 1));
	}
	
	/**
	 * Returns whether the given serial port is open or not. 
	 * @return true when it is open, false otherwise
	 */
	public boolean isOpen() {
		return isOpen;
	}
	
	
	/** Opens the given serial port. */
	public void open() {
		
		if (isOpen) {
			logger.warn("The port " + String.valueOf(port) + " is already open.");
			return;
		}
		
		logger.info("Open port " + String.valueOf(port));
		
		try {
			CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(port);
			CommPort port = portIdentifier.open(JeeLinkDevice.class.getName(), 2000);
			
			serialPort = (SerialPort) port;
			serialPort.setSerialPortParams(57600, SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
			
			input = serialPort.getInputStream();
			output = serialPort.getOutputStream();
			
			reader = new BufferedReader(new InputStreamReader(input));
			writer = new BufferedWriter(new OutputStreamWriter(output));
				
			serialPort.notifyOnDataAvailable(true);
			serialPort.addEventListener(this);
			
			this.isOpen = true;
			
		} catch (NoSuchPortException e) {
			logger.error("Could not find port " + port, e);
			
		} catch (PortInUseException e) {
			logger.error("Port " + port + "is already in use.", e);
			
		} catch (UnsupportedCommOperationException e) {
			logger.error("Failed to setup port " + port, e);
			
		} catch (IOException e) {
			logger.error("Failed to open port " + port, e);
			
		} catch (TooManyListenersException e) {
			logger.error("Internal error", e);
		}
		
		synchronized (pendingTasks) {
			executor = Executors.newSingleThreadScheduledExecutor();
		}
		
		// enable quite mode
		sendMessage("1q");
	}
	
	/** Opens the given serial port. */
	public void close() {
		
		if (!isOpen) {
			logger.warn("The port " + String.valueOf(port) + " is already closed.");
			return;
		}
		
		logger.info("Close port " + String.valueOf(port));
		
		isOpen = false;
		
		// stop retry tasks
		synchronized (pendingTasks) {
			for (Future<?> future : pendingTasks.values()) {
				future.cancel(false);
			}
			pendingTasks.clear();
			
			if (executor != null) {
				executor.shutdownNow();
				executor = null;
			}
		}
		
				
		if (serialPort != null) {
			serialPort.removeEventListener();
		}
		
		try {
			if (reader != null) {
				reader.close();
			}
		} catch (IOException e) {
			logger.error("Failed to close serial reader", e);
		}
		
		try {
			if (writer != null) {
				writer.close();
			}
		} catch (IOException e) {
			logger.error("Failed to close serial writer", e);
		}
		
		if (serialPort != null) {
			serialPort.close();
		}
	}
	
	/**
	 * Adds the specified JeeLink listener to receive device events.
	 * @param listener the JeeLink listener
	 */
	public void addListener(JeeLinkListener listener) {
		
		synchronized (listeners) {
			listeners.add(listener);
		}
	}
	
	/**
	 * Removes the specified JeeLink listener so that it no longer receives device events.
	 * @param listener the JeeLink listener
	 */
	public void removeListener(JeeLinkListener listener) {
	
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}
	
	/**
	 * Sends a command to list all known PCA301 devices.
	 * For each device every listener will receive a event.
	 */
	public void listDevices() {
		
		logger.info("List devices");
		sendMessage("l");
	}
	
	/**
	 * Change the switching state of device with specified address and channel.
	 * @param address PCA301 device address
	 * @param channel communication channel
	 * @param state new switching state
	 */
	public void setState(int address, int channel, boolean state) {
		
		logger.debug("Change state of " + String.valueOf(address) + " to " + String.valueOf(state));
		final int param = state ? JeeLinkMessage.PARAM_ON : JeeLinkMessage.PARAM_OFF; 
		final JeeLinkMessage msg = new JeeLinkMessage(address, channel, JeeLinkMessage.CMD_STATE, param);
		
		sendMessage(msg, retryCount);
	}
	
	/**
	 * Sends a command to device with specified address and channel to response current values.
	 * @param address PCA301 device address
	 * @param channel communication channel
	 */
	public void refresh(int address, int channel) {
		
		logger.debug("Refresh values of " + String.valueOf(address));
		final JeeLinkMessage msg = new JeeLinkMessage(address, channel, JeeLinkMessage.CMD_VALUES, JeeLinkMessage.PARAM_NONE);
		sendMessage(msg);
	}
	
	/**
	 * Resets the total consumption of device with specified address and channel
	 * @param address PCA301 device address
	 * @param channel communication channel
	 */
	public void resetConsumption(int address, int channel) {
		
		logger.debug("Reset values of " + String.valueOf(address));
		final JeeLinkMessage msg = new JeeLinkMessage(address, channel, JeeLinkMessage.CMD_VALUES, JeeLinkMessage.PARAM_RESET);
		sendMessage(msg);
	}
	
	/**
	 * Send the specified message with JeeLink device
	 * @param msg Message to PCA301 device
	 * @param retryCount Number of maximal retries. With zero or less behavior is same as {@link #sendMessage(JeeLinkMessage)}.
	 */
	protected void sendMessage(JeeLinkMessage msg, int retryCount) {
		
		if (retryCount > 0) {
			
			// create retry task
			final Runnable task = new RetrySendTask(this, msg, retryCount);
			
			synchronized (pendingTasks) {
				
				if (executor != null) {
					
					logger.debug("Create pending task for address=" + msg.getAddress() + " cmd=" + msg.getCommand());
					
					// schedule task and cancel old one for same message
					final Future<?> future = executor.schedule(task, RETRY_DELAY, TimeUnit.SECONDS);
					final Future<?> old = pendingTasks.put(msg, future);
					if (old != null) {
						old.cancel(false);
					}
				}
			}
		}
		
		sendMessage(msg);
	}
	
	/**
	 * Send the specified message with JeeLink device
	 * @param msg Message to PCA301 device
	 */
	protected void sendMessage(JeeLinkMessage msg) {
		
		final String text = msg.toSerialString();
		sendMessage(text);
	}
	
	private void sendMessage(String msg) {
		
		if (!isOpen) {
			logger.error("Not connected to JeeLink device");
			return;
		}
		
		try {
			logger.trace("Send message " + msg);
			writer.write(msg);
			writer.flush();
						
		} catch (IOException e) {
			logger.error("Failed to send message " + msg, e);
		}
	}

	@Override
	public void serialEvent(SerialPortEvent event) {
		
		if (event.getEventType() != SerialPortEvent.DATA_AVAILABLE) {
			return;
		}
		
		if (isOpen) {
			receiveMessage();
		}
	}
	
	private void receiveMessage() {
		
		try {
			String data = reader.readLine();
			if (data == null) {
				logger.error("Reach EOF on serial port " + port);
				return;
			}
			
			logger.trace("Received raw data: " + data);
			
			JeeLinkMessage msg = null;
			
			// filter converts data to message object
			for (JeeLinkFilter filter : filters) {
				msg = filter.processLine(data);
				if (msg != null) {
					break;
				}
			}
			
			if (msg != null) {
				
				clearPendingTasks(msg);
				
				final int cmd = msg.getCommand();
				final boolean state = msg.getParameter() == JeeLinkMessage.PARAM_ON ? true : false;
				
				switch (cmd) {
				case JeeLinkMessage.CMD_STATE:
					// message with current state only
					synchronized (listeners) {
						for (JeeLinkListener listener : listeners) {
							listener.onStateReceived(msg.getAddress(), msg.getChannel(), state);
						}
					}
					break;
				case JeeLinkMessage.CMD_VALUES:
					// message with current state and values
					synchronized (listeners) {
						for (JeeLinkListener listener : listeners) {
							listener.onStateReceived(msg.getAddress(), msg.getChannel(), state);
							listener.onValuesReceived(msg.getAddress(), msg.getChannel(), msg.getPower(), msg.getConsumption());
						}
					}
					break;
				case JeeLinkMessage.CMD_PAIRING:
					logger.info("Paired device with address " + msg.getAddress());
					listDevices();
					break;
				default:
					logger.warn("Ignore message with command " + String.valueOf(cmd));
					break;
				}
			}
			
		} catch (IOException e) {
			logger.error("Failed to read content on serial port " + port, e);
			
		} catch (ParseException e) {
			logger.error("Failed to parse message", e);
		
		} catch (InvalidSketchException e) {
			logger.error("JeeLink device on port " + port + " has a invalid sketch.", e);
			close();
						
		} catch (Exception e) {
			logger.error("Unknown error", e);
		}
		
	}
	
	private void clearPendingTasks(final JeeLinkMessage msg) {
		
		final Set<JeeLinkMessage> keys;
		synchronized (pendingTasks) {
			
			if (pendingTasks.isEmpty()) {
				return;
			}
			
			keys = new HashSet<JeeLinkMessage>(pendingTasks.size());
			keys.addAll(pendingTasks.keySet());
		}
		
		for (JeeLinkMessage key : keys) {
			if ((key.getAddress() == msg.getAddress()) && (key.getCommand() == msg.getCommand()) && (key.getParameter() == msg.getParameter())) {
				
				synchronized (pendingTasks) {
					
					logger.debug("Remove pending task for address=" + msg.getAddress() + " cmd=" + msg.getCommand());
					final Future<?> future = pendingTasks.remove(key);
					if (future != null) {
						future.cancel(false);
					}
				}
			}
		}
		
	}
}
