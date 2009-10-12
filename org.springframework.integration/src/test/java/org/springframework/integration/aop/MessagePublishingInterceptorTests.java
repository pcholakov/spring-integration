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

package org.springframework.integration.aop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.integration.channel.MapBasedChannelResolver;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.Message;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class MessagePublishingInterceptorTests {

	private final ExpressionSource source = new TestExpressionSource();

	private final MapBasedChannelResolver channelResolver = new MapBasedChannelResolver();

	private final QueueChannel testChannel = new QueueChannel();


	@Before
	public void setup() {
		channelResolver.setChannelMap(Collections.singletonMap("c", testChannel));
	}

	@Test
	public void returnValue() {
		MessagePublishingInterceptor interceptor = new MessagePublishingInterceptor(source);
		interceptor.setChannelResolver(channelResolver);
		ProxyFactory pf = new ProxyFactory(new TestBeanImpl());
		pf.addAdvice(interceptor);
		TestBean proxy = (TestBean) pf.getProxy();
		proxy.test();
		Message<?> message = testChannel.receive(0);
		assertNotNull(message);
		assertEquals("foo", message.getPayload());
	}
	@Test
	public void demoMethodNameMappingExpressionSource() {
		Map<String, String> expressionMap = new HashMap<String, String>();
		expressionMap.put("test", "#return");
		MethodNameMappingExpressionSource source = new MethodNameMappingExpressionSource(expressionMap);
		Map<String, String> channelMap = new HashMap<String, String>();
		channelMap.put("test", "c");
		source.setChannelMap(channelMap);
		
		Map<String, String[]> headerExpressionMap = new HashMap<String, String[]>();
		headerExpressionMap.put("test", new String[]{"bar=#return","name='oleg'"});
		source.setHeaderExpressionMap(headerExpressionMap);
		
		
		MessagePublishingInterceptor interceptor = new MessagePublishingInterceptor(source);
		interceptor.setChannelResolver(channelResolver);
		ProxyFactory pf = new ProxyFactory(new TestBeanImpl());
		pf.addAdvice(interceptor);
		TestBean proxy = (TestBean) pf.getProxy();
		proxy.test();
		Message<?> message = testChannel.receive(0);
		assertNotNull(message);
		assertEquals("foo", message.getPayload());
		assertEquals("foo", message.getHeaders().get("bar"));
		assertEquals("oleg", message.getHeaders().get("name"));
	}


	static interface TestBean {

		String test();

	}


	static class TestBeanImpl implements TestBean {

		public String test() {
			return "foo";
		}

	}

	private static class TestExpressionSource implements ExpressionSource {

		public String getArgumentMapName(Method method) {
			return "m";
		}

		public String[] getArgumentNames(Method method) {
			return new String[] { "a1", "a2"};
		}

		public String getReturnValueName(Method method) {
			return "r";
		}

		public String getExceptionName(Method method) {
			return "x";
		}

		public String getPayloadExpression(Method method) {
			return "#r";
		}

		public String[] getHeaderExpressions(Method method) {
			return null;
		}

		public String getChannelName(Method method) {
			return "c";
		}
	}

}