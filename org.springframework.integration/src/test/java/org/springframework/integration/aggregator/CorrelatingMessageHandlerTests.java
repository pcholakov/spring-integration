/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.aggregator;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.core.MessageHeaders;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.store.MessageStore;

/**
 * @author Iwein Fuld
 */
@RunWith(MockitoJUnitRunner.class)
public class CorrelatingMessageHandlerTests {

	private CorrelatingMessageHandler handler;

	@Mock
	private MessageStore store;

	@Mock
	private CorrelationStrategy correlationStrategy;

	@Mock
	private CompletionStrategy completionStrategy;

	@Mock
	private MessageGroupProcessor processor;

	@Mock
	private MessageChannel outputChannel;

	@Before
	public void initializeSubject() {
		handler = new CorrelatingMessageHandler(
				store, correlationStrategy, completionStrategy, processor);
		handler.setOutputChannel(outputChannel);
	}

	@Test
	public void bufferCompletesNormally() throws Exception {
		String correlationKey = "key";
		Message<?> message1 = testMessage(1, 1);
		Message<?> message2 = testMessage(2, 2);
		List<Message<?>> storedMessages = new ArrayList<Message<?>>();

		when(store.list(correlationKey)).thenReturn(storedMessages);

		when(correlationStrategy.getCorrelationKey(isA(Message.class)))
				.thenReturn(correlationKey);

		when(completionStrategy.isComplete(storedMessages)).thenReturn(false);

		handler.handleMessage(message1);
		storedMessages.add(message1);

		when(completionStrategy.isComplete(storedMessages)).thenReturn(true);
		handler.handleMessage(message2);
		storedMessages.add(message2);

		verify(store).put(message1);
		verify(store).put(message2);
		verify(store, times(2)).list(correlationKey);
		verify(correlationStrategy).getCorrelationKey(message1);
		verify(correlationStrategy).getCorrelationKey(message2);
		verify(completionStrategy, times(2)).isComplete(storedMessages);
		verify(processor).processAndSend(eq(correlationKey),
				eq(storedMessages), eq(outputChannel),
				isA(BufferedMessagesCallback.class));
	}


	private Message<?> testMessage(int id, int sequenceNumber) {
		return MessageBuilder.withPayload("test")
				.setHeader(MessageHeaders.ID, id)
				.setSequenceNumber(sequenceNumber).build();
	}

}