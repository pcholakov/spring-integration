/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.ip.tcp;

import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ThreadFactory;

import org.springframework.integration.ip.AbstractInternetProtocolReceivingChannelAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Abstract class for tcp/ip incoming channel adapters. Implementations
 * for {@link java.net.Socket} and {@link java.nio.channels.SocketChannel}
 * are provided.
 * 
 * @author Gary Russell
 *
 */
public abstract class AbstractTcpReceivingChannelAdapter extends
		AbstractInternetProtocolReceivingChannelAdapter {

	protected volatile ThreadPoolTaskScheduler threadPoolTaskScheduler;

	protected volatile int poolSize = -1;
	
	protected volatile SocketMessageMapper mapper = new SocketMessageMapper();

	protected volatile boolean soKeepAlive;

	protected int messageFormat = MessageFormats.FORMAT_LENGTH_HEADER;
	
	protected Class<SocketReader> customSocketReader;
	
	protected boolean usingDirectBuffers; 
	
	/**
	 * Constructs a receiving channel adapter that listens on the port.
	 * @param port The port to listen on.
	 */
	public AbstractTcpReceivingChannelAdapter(int port) {
		super(port);
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.endpoint.AbstractEndpoint#doStop()
	 */
	@Override
	protected void doStop() {
		// TODO Auto-generated method stub

	}

	/**
	 * Creates the ThreadPoolTaskScheduler, if necessary, and calls 
	 * {@link #server()}.
	 */
	public void run() {
		if (logger.isDebugEnabled()) {
			logger.debug(this.getClass().getSimpleName() + " running...");
		}
		if (this.active && this.threadPoolTaskScheduler == null) {
			this.threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
			this.threadPoolTaskScheduler.setThreadFactory(new ThreadFactory() {
				public Thread newThread(Runnable runner) {
					Thread thread = new Thread(runner);
					thread.setName("TCP-Incoming-Msg-Handler");
					thread.setDaemon(true);
					return thread;
				}
			});
			if (this.poolSize > 0) {
				this.threadPoolTaskScheduler.setPoolSize(this.poolSize);
			}
			this.threadPoolTaskScheduler.initialize();
		}
		server();
	}

	/**
	 * Establishes the server.
	 */
	protected abstract void server();

	/**
	 * Sets soTimeout, soKeepAlive and tcpNoDelay according to the configured
	 * properties.
	 * @param socket The socket.
	 * @throws SocketException
	 */
	protected void setSocketOptions(Socket socket) throws SocketException {
		socket.setSoTimeout(this.soTimeout);
		if (this.soReceiveBufferSize > 0) {
			socket.setReceiveBufferSize(this.soReceiveBufferSize);
		}
		socket.setKeepAlive(this.soKeepAlive);
		
	}

	/**
	 * @param soKeepAlive the soKeepAlive to set
	 */
	public void setSoKeepAlive(boolean soKeepAlive) {
		this.soKeepAlive = soKeepAlive;
	}

	/**
	 * @param messageFormat the messageFormat to set
	 */
	public void setMessageFormat(int messageFormat) {
		this.messageFormat = messageFormat;
	}

	/**
	 * @param customSocketReader the customSocketReader to set
	 * @throws ClassNotFoundException 
	 */
	@SuppressWarnings("unchecked")
	public void setCustomSocketReader(String customSocketReaderClassName) throws ClassNotFoundException {
		this.customSocketReader = (Class<SocketReader>) Class.forName(customSocketReaderClassName);
	}

	/**
	 * @param usingDirectBuffers the usingDirectBuffers to set
	 */
	public void setUsingDirectBuffers(boolean usingDirectBuffers) {
		this.usingDirectBuffers = usingDirectBuffers;
	}

	/**
	 * @param poolSize the poolSize to set
	 */
	public void setPoolSize(int poolSize) {
		this.poolSize = poolSize;
	}

}