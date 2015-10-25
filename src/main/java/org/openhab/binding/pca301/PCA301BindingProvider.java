/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.pca301;

import org.openhab.core.binding.BindingProvider;

/**
 * Interface to provide a binding for ELV PCA-301 devices.
 * @author ribbeck
 * @since 1.7.2
 */
public interface PCA301BindingProvider extends BindingProvider {
	
	/**
	 * Returns the name of the item with passed address and property.
	 * @param address device address
	 * @param property name of the property
	 * @return if found the item name, null otherwise
	 */
	public String getItemName(int address, String property);
	
	/**
	 * Returns the device address for the item with passed name.
	 * @param itemName name of the item
	 * @return device address or null if not found
	 */
	public int getAddress(String itemName);
	
	/**
	 * Returns the property for the item with passed name.
	 * @param itemName name of the item
	 * @return property name or null if not found
	 */
	public String getProperty(String itemName);
}
