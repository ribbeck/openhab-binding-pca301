package org.openhab.binding.pca301.internal.jeelink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Task which retry to send a message with decreased retry count.
 * @author ribbeck
 * @since 1.7.2
 */
public class RetrySendTask implements Runnable {

	private final static Logger logger = LoggerFactory.getLogger(RetrySendTask.class);
	
	private final JeeLinkDevice dev;
	private final JeeLinkMessage msg;
	private final int count;
	
	/**
	 * Constructor
	 * @param device JeeLink device
	 * @param message command message which should be send
	 * @param count current retry count 
	 */
	public RetrySendTask(JeeLinkDevice device, JeeLinkMessage message, int count) {
		this.dev = device;
		this.msg = message;
		this.count = count;
	}

	@Override
	public void run() {
		logger.info("Command " + msg.getCommand() +" for address " + msg.getAddress() + " failed. Start retry.");
		dev.sendMessage(msg, (count - 1));
	}
}
