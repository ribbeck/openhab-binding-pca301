/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.pca301.test;

import org.openhab.binding.pca301.internal.jeelink.JeeLinkDevice;
import org.openhab.binding.pca301.internal.jeelink.JeeLinkListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Class to test and debug the JeeLink device. */
public class DebugJeeLink {

	static final String port = "/dev/ttyUSB0";
	static final Logger logger = LoggerFactory.getLogger(DebugJeeLink.class);
	
	static final Object lock = new Object(); 
	
	static int devAddress = 0;
	static int devChannel = 0;

	
	public static void main(String[] args) {
		
		final JeeLinkListener listener = new JeeLinkListener() {
			
			@Override
			public void onValuesReceived(int address, int channel, double power, double consumption) {
				logger.info(String.format("device: %d, channel: %d, power: %f W, consumption: %f kWh", address, channel, power, consumption));
				
				synchronized (lock) {
					
					// use first PCA301 device
					if (devAddress == 0) {
						devAddress = address;
						devChannel = channel;
					}
					
					lock.notifyAll();
				}
			}
			
			@Override
			public void onStateReceived(int address, int channel, boolean state) {
				logger.info(String.format("device: %d, channel: %d, state: %b", address, channel, state));
			}
		};
		
		
		logger.info("Create and open device on port " + port);
		
		final JeeLinkDevice dev = new JeeLinkDevice(port);
		dev.addListener(listener);
		dev.open();
		
		try {
			
			// wait until we received list of known PCA301 devices
			synchronized (lock) {
				lock.wait(5000);
			}
						
			if (devAddress != 0) {
				// switch first device on
				logger.info("send command to address=" + String.valueOf(devAddress));
				dev.setState(devAddress, devChannel, true);
			}
			
			synchronized (lock) {
				lock.wait(2000);
			}
		
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		
		dev.removeListener(listener);
		dev.close();
		
		logger.info("Device on port " + port + " is closed.");
	}

}
