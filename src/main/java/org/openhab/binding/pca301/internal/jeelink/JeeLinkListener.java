/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.pca301.internal.jeelink;

/**
 * Interface to delegate events from JeeLink device. 
 * @author ribbeck
 * @since 1.7.2
 */
public interface JeeLinkListener {

	/**
	 * Will be called when a new state message received.
	 * @param address PCA301 device address
	 * @param channel communication channel
	 * @param state current switching state, true when on, flase otherwise
	 */
	void onStateReceived(int address, int channel, boolean state);
	
	/**
	 * Will be called when a new values message received.
	 * @param address PCA301 device address
	 * @param channel communication channel
	 * @param power current power in W (watt)
	 * @param consumption total consumption in kWh (kilowatt hour)
	 */
	void onValuesReceived(int address, int channel, double power, double consumption);
	
}
