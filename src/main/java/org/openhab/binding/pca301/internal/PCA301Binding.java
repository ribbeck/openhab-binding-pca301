/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.pca301.internal;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.openhab.binding.pca301.PCA301BindingProvider;
import org.openhab.binding.pca301.internal.PCA301BindingConfig.Property;
import org.openhab.binding.pca301.internal.jeelink.JeeLinkDevice;
import org.openhab.binding.pca301.internal.jeelink.JeeLinkListener;
import org.openhab.core.binding.AbstractBinding;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Binding class which connects openHAB with PCA301 devices.
 * 
 * @author ribbeck
 * @since 1.7.2
 */
public class PCA301Binding extends AbstractBinding<PCA301BindingProvider> implements ManagedService, JeeLinkListener {
	
	private static final Logger logger = LoggerFactory.getLogger(PCA301Binding.class);
	
	private final static String KEY_PORT = "port";
	private final static String KEY_RETRY_COUNT = "retryCount";
	
	private final Map<Integer, Integer> channels = new HashMap<Integer, Integer>();
	
	private final Map<String, State> cache = new HashMap<String, State>();
	
	
	JeeLinkDevice device = null;


	@Override
	public void activate() {
		logger.trace("activate() called");
		if (device != null) {
			device.open();
		}
	}

	@Override
	public void deactivate() {
		logger.trace("deactivate() called");
		if (device != null) {
			device.close();
		}
	}

	/**
	 * @{inheritDoc
	 */
	@Override
	protected void internalReceiveCommand(String itemName, Command command) {
		
		logger.trace("internalReceiveCommand(" + itemName + ", " + command + ") called");
		if (command instanceof OnOffType) {
			
			final OnOffType switchValue = (OnOffType)command;
			executeCommand(itemName, switchValue == OnOffType.ON);
		}
	}

	/**
	 * @{inheritDoc
	 */
	@Override
	protected void internalReceiveUpdate(String itemName, State newState) {
		
		logger.trace("internalReceiveUpdate(" + itemName + ", " + newState + ") called");
		if (newState instanceof OnOffType) {
			
			final OnOffType switchValue = (OnOffType)newState;
			executeCommand(itemName, switchValue == OnOffType.ON);
		}
	}
	
	private void executeCommand(String itemName, boolean value) {
		
		for (PCA301BindingProvider provider : providers) {
			
			// find address and channel of item
			final int address = provider.getAddress(itemName);
			if (address != 0) {
				Integer channel = null; 
				synchronized (channels) {
					channel = channels.get(Integer.valueOf(address));
				}
				
				if (channel == null) {
					logger.warn("Unknown address: " + String.valueOf(address));
					return;
				}
				
				// get property
				final String propertyName = provider.getProperty(itemName);
				if (propertyName == null) {
					logger.error("Cannot find property of item " + itemName);
					return;
				}
				
				logger.trace("Send " + propertyName + " to address=" + address + ", channnel=" + channel);
				
				Property property = Property.UNKNOWN;
				try {
					property = Property.valueOf(propertyName.toUpperCase());
					
				} catch (IllegalArgumentException e) {
					logger.error("Unknown property: " + propertyName);
					return;
				}
								
				// send command
				if (device != null) {
					
					switch (property) {
					case RESET:
						if (value) {
							device.resetConsumption(address, channel);
						}
						break;
					case STATE:
						device.setState(address, channel, value);
						break;
					default:
						logger.warn("Invalid property: " + propertyName);
						break;
					}
					
				}
			}
		}
	}

	/**
	 * @{inheritDoc
	 */
	public void updated(Dictionary<String, ?> config) throws ConfigurationException {
		logger.debug("Received new config");
		if (config != null) {

			// configuration has changed, close JeeLink device if necessary
			if (device != null) {
				
				if (device.isOpen()) {
					device.close();
				}
				device.removeListener(this);
				device = null;
			}
			
			// reset channels and cache
			synchronized (channels) {
				channels.clear();
			}
			synchronized (cache) {
				cache.clear();
			}
			
			// read serial port name
			final String port = (String) config.get(KEY_PORT);
			if (StringUtils.isBlank(port)) {
				logger.error("Port of JeeLink device is missing");
				throw new ConfigurationException(KEY_PORT, "The port can't be empty");
			}
			
			// read retry count, default is zero
			int retryCount = 0;
			final String retryCountValue = (String) config.get(KEY_RETRY_COUNT);
			if (StringUtils.isNotBlank(retryCountValue)) {
				try {
					retryCount = Integer.parseInt(retryCountValue);
					
				} catch (final NumberFormatException e) {
					logger.error("Failed to read retry count value: " + retryCountValue, e);
				}
			}
			
			// create and open JeeLink device
			device = new JeeLinkDevice(port, retryCount);
			device.addListener(this);
			device.open();
		}
	}

	@Override
	public void onStateReceived(int address, int channel, boolean state) {
		
		synchronized (channels) {
			channels.put(Integer.valueOf(address), Integer.valueOf(channel));
		}
		
		logger.debug("Received for " + String.valueOf(address) + " state=" + (state ? "on" : "off"));
		publishUpdate(address, Property.STATE, state ? OnOffType.ON : OnOffType.OFF);
	}

	@Override
	public void onValuesReceived(int address, int channel, double power, double consumption) {
		
		synchronized (channels) {
			channels.put(Integer.valueOf(address), Integer.valueOf(channel));
		}
		
		final String powerValue = Double.toString(power);
		final String consumptionValue = Double.toString(consumption);
		
		logger.debug("Received for " + String.valueOf(address) + " power=" + powerValue + " and consumption=" + consumptionValue);
		publishUpdate(address, Property.POWER, DecimalType.valueOf(powerValue));
		publishUpdate(address, Property.CONSUMPTION, DecimalType.valueOf(consumptionValue));
	}
	
	/**
	 * Sends an update event to openHAB for passed value if necessary.
	 * @param address PCA301 device address
	 * @param property PCA301 property
	 * @param newValue new value of property
	 */
	private void publishUpdate(int address, Property property, State newValue) {
		
		for (PCA301BindingProvider provider : providers) {
			
			// check if item exists for passed property
			final String itemName = provider.getItemName(address, property.toString());
			if (itemName != null) {
				
				// get and refresh current state
				State currentValue = null;
				synchronized (cache) {
					currentValue = cache.put(itemName, newValue);
				}
				
				if (!newValue.equals(currentValue)) {
					// value has changed
					eventPublisher.postUpdate(itemName, newValue);
				}
			}
		}
	}
}
