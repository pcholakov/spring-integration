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

import static org.junit.Assert.assertEquals;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.net.ServerSocketFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.channel.SubscribableChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.ip.util.SocketUtils;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class SimpleTcpNetOutboundGatewayTests {

	@Autowired
	@Qualifier("gatewayCrLf")
	private SimpleTcpNetInboundGateway inboundGatewayCrLf;
	
	@Autowired
	@Qualifier("gatewayStxEtx")
	private SimpleTcpNetInboundGateway inboundGatewayStxEtx;
	
	@Autowired
	@Qualifier("gatewayLength")
	private SimpleTcpNetInboundGateway inboundGatewayLength;

	@Autowired
	@Qualifier("gatewaySerialized")
	private SimpleTcpNetInboundGateway inboundGatewaySerialized;

	@Autowired
	@Qualifier("gatewayCustom")
	private SimpleTcpNetInboundGateway inboundGatewayCustom;
	
	@Autowired
	@Qualifier("requestChannel")
	SubscribableChannel requestChannel;

	@Autowired
	@Qualifier("replyChannel")
	PollableChannel replyChannel;

	@Test
	public void testOutboundCrLf() throws Exception {
		SimpleTcpNetOutboundGateway gateway = new SimpleTcpNetOutboundGateway
			("localhost", inboundGatewayCrLf.getPort());
		gateway.setMessageFormat(MessageFormats.FORMAT_CRLF);
		waitListening(inboundGatewayCrLf);
		Message<String> message = MessageBuilder.withPayload("test").build();
		byte[] bytes = (byte[]) gateway.handleRequestMessage(message);
		assertEquals("echo:test", new String(bytes));
	}

	private void waitListening(SimpleTcpNetInboundGateway gateway) throws Exception {
		int n = 0;
		while (!gateway.isListening()) {
			Thread.sleep(100);
			if (n++ > 100) {
				throw new Exception("Gateway failed to listen");
			}
		}
		
	}

	@Test
	public void testOutboundStxEtx() throws Exception {
		SimpleTcpNetOutboundGateway gateway = new SimpleTcpNetOutboundGateway
			("localhost", inboundGatewayStxEtx.getPort());
		gateway.setMessageFormat(MessageFormats.FORMAT_STX_ETX);
		waitListening(inboundGatewayStxEtx);
		Message<String> message = MessageBuilder.withPayload("test").build();
		byte[] bytes = (byte[]) gateway.handleRequestMessage(message);
		assertEquals("echo:test", new String(bytes));
	}

	@Test
	public void testOutboundSerialized() throws Exception {
		SimpleTcpNetOutboundGateway gateway = new SimpleTcpNetOutboundGateway
			("localhost", inboundGatewaySerialized.getPort());
		gateway.setMessageFormat(MessageFormats.FORMAT_JAVA_SERIALIZED);
		waitListening(inboundGatewaySerialized);
		Message<String> message = MessageBuilder.withPayload("test").build();
		Object response = gateway.handleRequestMessage(message);
		assertEquals("echo:test", response);
	}

	@Test
	public void testOutboundLength() throws Exception {
		SimpleTcpNetOutboundGateway gateway = new SimpleTcpNetOutboundGateway
			("localhost", inboundGatewayLength.getPort());
		gateway.setMessageFormat(MessageFormats.FORMAT_LENGTH_HEADER);
		waitListening(inboundGatewayLength);
		Message<String> message = MessageBuilder.withPayload("test").build();
		byte[] bytes = (byte[]) gateway.handleRequestMessage(message);
		assertEquals("echo:test", new String(bytes));
	}

	@Test
	public void testOutboundCustom() throws Exception {
		SimpleTcpNetOutboundGateway gateway = new SimpleTcpNetOutboundGateway
			("localhost", inboundGatewayCustom.getPort());
		gateway.setMessageFormat(MessageFormats.FORMAT_CUSTOM);
		gateway.setCustomSocketReaderClassName("org.springframework.integration.ip.tcp.CustomNetSocketReader");
		gateway.setCustomSocketWriterClassName("org.springframework.integration.ip.tcp.CustomNetSocketWriter");
		waitListening(inboundGatewayCustom);
		Message<String> message = MessageBuilder.withPayload("test").build();
		byte[] bytes = (byte[]) gateway.handleRequestMessage(message);
		assertEquals("echo:test", new String(bytes).trim());
	}
	
	@Test
	public void testOutboundUsingConfig() {
		Message<String> message = MessageBuilder.withPayload("test").build();
		requestChannel.send(message);
		byte[] bytes = (byte[]) replyChannel.receive().getPayload();
		assertEquals("echo:test", new String(bytes).trim());
	}
	
	@Test
	public void testOutboundClose() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		final CountDownLatch latch1 = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);
		final CountDownLatch latch3 = new CountDownLatch(1);
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					ServerSocket ss = ServerSocketFactory.getDefault().createServerSocket(port);
					latch1.countDown();
					while (true) {
						Socket s = ss.accept();
						byte[] b = new byte[1024];
						s.getInputStream().read(b);
						s.getOutputStream().write("OK\r\n".getBytes());
						latch2.countDown();
						latch3.await();
						s.close();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}});
		t.start();
		latch1.await(2000, TimeUnit.MILLISECONDS);
		SimpleTcpNetOutboundGateway gateway = new SimpleTcpNetOutboundGateway
			("localhost", port);
		gateway.setMessageFormat(MessageFormats.FORMAT_CRLF);
		gateway.setClose(true);
		Message<String> message = MessageBuilder.withPayload("test").build();
		byte[] bytes = (byte[]) gateway.handleRequestMessage(message);
		assertEquals("OK", new String(bytes));
		latch3.countDown();
		latch2.await(2000, TimeUnit.MILLISECONDS);
		bytes = (byte[]) gateway.handleRequestMessage(message);
		assertEquals("OK", new String(bytes));
	}
	
}