/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.pca301.internal;

import java.util.Map.Entry;

import org.openhab.binding.pca301.PCA301BindingProvider;
import org.openhab.core.binding.BindingConfig;
import org.openhab.core.items.Item;
import org.openhab.model.item.binding.AbstractGenericBindingProvider;
import org.openhab.model.item.binding.BindingConfigParseException;


/**
 * This class is responsible for parsing the binding configuration.
 * 
 * @author ribbeck
 * @since 1.7.2
 */
public class PCA301GenericBindingProvider extends AbstractGenericBindingProvider implements PCA301BindingProvider {

	/**
	 * {@inheritDoc}
	 */
	public String getBindingType() {
		return "pca301";
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public void validateItemType(Item item, String bindingConfig) throws BindingConfigParseException {
		//if (!(item instanceof SwitchItem || item instanceof DimmerItem)) {
		//	throw new BindingConfigParseException("item '" + item.getName()
		//			+ "' is of type '" + item.getClass().getSimpleName()
		//			+ "', only Switch- and DimmerItems are allowed - please check your *.items configuration");
		//}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void processBindingConfiguration(String context, Item item, String bindingConfig) throws BindingConfigParseException {
		super.processBindingConfiguration(context, item, bindingConfig);
		
		// add binding config
		final PCA301BindingConfig config = PCA301BindingConfig.parse(bindingConfig);
		addBindingConfig(item, config);
	}
	
	@Override
	public String getItemName(int address, String property) {
		for (Entry<String, BindingConfig> entry : bindingConfigs.entrySet()) {
			
			final PCA301BindingConfig config = (PCA301BindingConfig)entry.getValue();
			if ((address == config.getAddress()) && property.equals(config.getPropertyName())) {
				return entry.getKey();
			}
		}
		
		return null;
	}

	@Override
	public int getAddress(String itemName) {
		
		PCA301BindingConfig config = (PCA301BindingConfig)bindingConfigs.get(itemName);
		if (config != null) {
			return config.getAddress();
		}
		
		return 0;
	}
	
	@Override
	public String getProperty(String itemName) {
		
		PCA301BindingConfig config = (PCA301BindingConfig)bindingConfigs.get(itemName);
		if (config != null) {
			return config.getPropertyName();
		}
		
		return null;
	}
}
