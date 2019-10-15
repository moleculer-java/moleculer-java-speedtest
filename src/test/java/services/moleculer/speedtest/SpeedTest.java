/**
 * THIS SOFTWARE IS LICENSED UNDER MIT LICENSE.<br>
 * <br>
 * Copyright 2019 Andras Berkes [andras.berkes@programmer.net]<br>
 * Based on Moleculer Framework for NodeJS [https://moleculer.services].
 * <br><br>
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:<br>
 * <br>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.<br>
 * <br>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package services.moleculer.speedtest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.eventbus.EventBus;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageCodec;
import junit.framework.TestCase;
import services.moleculer.ServiceBroker;
import services.moleculer.eventbus.DefaultEventbus;
import services.moleculer.eventbus.Listener;
import services.moleculer.monitor.ConstantMonitor;
import services.moleculer.service.Service;
import services.moleculer.util.CheckedTree;

public class SpeedTest extends TestCase {

	// --- TEST PARAMETERS ---

	public static final int WARM_UP_LOOPS = 100;

	public static final int TEST_LOOPS = 100000;

	// --- EVENT BUSES AND LISTENERS ---

	protected ServiceBroker moleculerBroker;
	protected TestMoleculerService moleculerService;

	protected ServiceBroker moleculerBrokerAsync;
	protected TestMoleculerServiceAsync moleculerServiceAsync;

	protected AnnotationConfigApplicationContext springContext;
	protected TestSpringEventListener springListener;

	protected EventBus guavaEventBus;
	protected GuavaEventListener guavaListener;

	protected Vertx vertx;
	protected VertxListener vertxListener;

	protected ActorSystem akkaSystem;
	protected ActorRef akkaListenerRef;
	protected static CompletableFuture<Long> akkaPromise;
	protected static long akkaLimit;
	protected static AtomicLong akkaCounter;
	
	// --- VERIFICATION ---

	protected long checksumWarmUp;
	protected long checksumTest;

	// --- INIT ---

	@Override
	protected void setUp() throws Exception {

		// Create checksum
		for (int i = 0; i < WARM_UP_LOOPS; i++) {
			checksumWarmUp += i;
		}
		for (int i = 0; i < TEST_LOOPS; i++) {
			checksumTest += i;
		}

		// Create Moleculer Service Broker
		moleculerBroker = ServiceBroker.builder().monitor(new ConstantMonitor()).internalServices(false).build();
		moleculerService = new TestMoleculerService();
		moleculerBroker.createService(moleculerService);
		moleculerBroker.start();

		DefaultEventbus deb = new DefaultEventbus();
		deb.setAsyncLocalInvocation(true);
		moleculerBrokerAsync = ServiceBroker.builder().monitor(new ConstantMonitor()).eventbus(deb).internalServices(false).build();
		moleculerServiceAsync = new TestMoleculerServiceAsync();
		moleculerBrokerAsync.createService(moleculerServiceAsync);
		moleculerBrokerAsync.start();

		// Create Spring Context
		springContext = new AnnotationConfigApplicationContext(TestSpringConfig.class);
		springListener = springContext.getBean(TestSpringEventListener.class);

		// Create Google Guava EventBus
		guavaEventBus = new EventBus();
		guavaListener = new GuavaEventListener();
		guavaEventBus.register(guavaListener);

		// Create Vert.x Environment
		VertxOptions opts = new VertxOptions();
		opts.setEventLoopPoolSize(1);
		vertx = Vertx.vertx(opts);
		vertxListener = new VertxListener();
		io.vertx.core.eventbus.EventBus eventBus = vertx.eventBus();
		eventBus.registerDefaultCodec(VertxMessage.class, new VertxMessageCodec());
		eventBus.consumer("test.event", vertxListener);

		// Create Akka Inbox
		akkaSystem = ActorSystem.create("ServerEvents");
		akkaListenerRef = akkaSystem.actorOf(Props.create(AkkaListener.class), "test");

		// Warm up
		doMoleculerTest(WARM_UP_LOOPS, checksumWarmUp);
		doMoleculerTestAsync(WARM_UP_LOOPS, checksumWarmUp);
		doSpringTest(WARM_UP_LOOPS, checksumWarmUp);
		doGuavaTest(WARM_UP_LOOPS, checksumWarmUp);
		doVertxTest(WARM_UP_LOOPS, checksumWarmUp);
		doAkkaTest(WARM_UP_LOOPS, checksumWarmUp);
	}

	// --- DO TEST ---

	@Test
	public void testSpeed() throws Exception {

		// Do Moleculer Service Broker test (sync)
		long duration = doMoleculerTest(TEST_LOOPS, checksumTest);
		System.out.println("Moleculer Sync: " + duration + " msec / " + TEST_LOOPS + " events");

		// Do Moleculer Service Broker test (async)
		duration = doMoleculerTestAsync(TEST_LOOPS, checksumTest);
		System.out.println("Moleculer Async: " + duration + " msec / " + TEST_LOOPS + " events");

		// Do Moleculer Service Broker test
		duration = doSpringTest(TEST_LOOPS, checksumTest);
		System.out.println("Spring EventBus: " + duration + " msec / " + TEST_LOOPS + " events");

		// Do Google Guava EventBus test
		duration = doGuavaTest(TEST_LOOPS, checksumTest);
		System.out.println("Guava EventBus: " + duration + " msec / " + TEST_LOOPS + " events");

		// Do Vert.x EventBus test
		duration = doVertxTest(TEST_LOOPS, checksumTest);
		System.out.println("Vert.x EventBus: " + duration + " msec / " + TEST_LOOPS + " events");
		
		// Do Akka test
		duration = doAkkaTest(TEST_LOOPS, checksumTest);
		System.out.println("Akka EventBus: " + duration + " msec / " + TEST_LOOPS + " events");		
	}

	protected long doMoleculerTest(int loops, long checksum) throws Exception {
		moleculerService.counter.set(0);

		long start = System.currentTimeMillis();
		for (int i = 0; i < loops; i++) {
			moleculerBroker.broadcast("test.event", new CheckedTree(i));
		}
		long duration = System.currentTimeMillis() - start;
		assertEquals(checksum, moleculerService.counter.get());
		return duration;
	}

	protected long doMoleculerTestAsync(int loops, long checksum) throws Exception {
		moleculerServiceAsync.counter.set(0);
		moleculerServiceAsync.future = new CompletableFuture<Void>();
		moleculerServiceAsync.limit = checksum;
		
		// Asynchronous transport
		long start = System.currentTimeMillis();
		for (int i = 0; i < loops; i++) {
			moleculerBrokerAsync.broadcast("test.event", new CheckedTree(i));
		}
		moleculerServiceAsync.future.get();
		long duration = System.currentTimeMillis() - start;
		assertEquals(checksum, moleculerServiceAsync.counter.get());
		return duration;
	}
	
	protected long doSpringTest(int loops, long checksum) throws Exception {
		springListener.counter.set(0);

		long start = System.currentTimeMillis();
		for (int i = 0; i < loops; i++) {
			springContext.publishEvent(new TestSpringEvent(this, i));
		}
		long duration = System.currentTimeMillis() - start;
		assertEquals(checksum, springListener.counter.get());
		return duration;
	}

	protected long doGuavaTest(int loops, long checksum) throws Exception {
		guavaListener.counter.set(0);

		long start = System.currentTimeMillis();
		for (int i = 0; i < loops; i++) {
			guavaEventBus.post(new GuavaEvent(i));
		}
		long duration = System.currentTimeMillis() - start;
		assertEquals(checksum, guavaListener.counter.get());
		return duration;
	}

	protected long doVertxTest(int loops, long checksum) throws Exception {
		vertxListener.counter.set(0);
		io.vertx.core.eventbus.EventBus eventBus = vertx.eventBus();
		DeliveryOptions opts = new DeliveryOptions();
		opts.setLocalOnly(true);
		opts.setCodecName(VertxMessageCodec.class.getSimpleName());
		vertxListener.future = new CompletableFuture<>();
		vertxListener.limit = checksum;

		// Vertx is asynchronous
		long start = System.currentTimeMillis();
		for (int i = 0; i < loops; i++) {
			eventBus.send("test.event", new VertxMessage(i), opts);
		}
		vertxListener.future.get();
		long duration = System.currentTimeMillis() - start;

		assertEquals(checksum, vertxListener.counter.get());
		return duration;
	}

	protected long doAkkaTest(int loops, long checksum) throws Exception {
		ActorRef sender = ActorRef.noSender();
		akkaPromise = new CompletableFuture<Long>();
		akkaLimit = checksum;
		akkaCounter = new AtomicLong();
		long result;
		
		// Akka is asynchronous
		long start = System.currentTimeMillis();
		for (int i = 0; i < loops; i++) {
			akkaListenerRef.tell(new AkkaMessage(i), sender);
		}
		result = akkaPromise.get();
		long duration = System.currentTimeMillis() - start;

		assertEquals(checksum, result);
		return duration;
	}
	
	// --- TEAR DOWN ---

	@Override
	protected void tearDown() throws Exception {
		if (moleculerBroker != null) {
			moleculerBroker.stop();
		}
		if (springContext != null) {
			springContext.close();
		}
		if (vertx != null) {
			vertx.close();
		}
		if (akkaSystem != null) {
			akkaSystem.terminate();
		}
	}

	// --- LISTENER CLASSES ---

	protected static class TestMoleculerService extends Service {

		AtomicLong counter = new AtomicLong();

		@services.moleculer.eventbus.Subscribe("test.event")
		public Listener evt = ctx -> {
			counter.addAndGet(ctx.params.asInteger());
		};

	}

	protected static class TestMoleculerServiceAsync extends Service {

		AtomicLong counter = new AtomicLong();
		CompletableFuture<Void> future;
		long limit;
		
		@services.moleculer.eventbus.Subscribe("test.event")
		public Listener evt = ctx -> {
			if (counter.addAndGet(ctx.params.asInteger()) >= limit) {
				future.complete(null);
			};
		};

	}
	
	protected static class TestSpringEvent extends ApplicationEvent {

		private static final long serialVersionUID = 1L;

		public final int delta;

		public TestSpringEvent(Object source, int delta) {
			super(source);
			this.delta = delta;
		}

	}

	protected static class TestSpringEventListener implements ApplicationListener<TestSpringEvent> {

		AtomicLong counter = new AtomicLong();

		@Override
		public void onApplicationEvent(TestSpringEvent event) {
			counter.addAndGet(event.delta);
		}

	}

	@Configuration
	protected static class TestSpringConfig {

		@Bean
		public TestSpringEventListener testSpringEventListener() {
			return new TestSpringEventListener();
		}

	}

	protected static class GuavaEventListener {

		AtomicLong counter = new AtomicLong();

		@com.google.common.eventbus.Subscribe()
		public void intEvent(GuavaEvent event) {
			counter.addAndGet(event.delta);
		}

	}

	protected static class GuavaEvent {

		public final int delta;

		public GuavaEvent(int delta) {
			this.delta = delta;
		}

	}

	protected static class VertxMessageCodec implements MessageCodec<VertxMessage, VertxMessage> {

		@Override
		public void encodeToWire(Buffer buffer, VertxMessage s) {
			buffer.appendInt(s.delta);
		}

		@Override
		public VertxMessage decodeFromWire(int pos, Buffer buffer) {
			return new VertxMessage(buffer.getInt(pos));
		}

		@Override
		public VertxMessage transform(VertxMessage s) {
			return s;
		}

		@Override
		public String name() {
			return this.getClass().getSimpleName();
		}

		@Override
		public byte systemCodecID() {
			return -1;
		}

	}

	protected static class VertxMessage {

		public final int delta;

		public VertxMessage(int delta) {
			this.delta = delta;
		}

	}

	protected static class VertxListener implements io.vertx.core.Handler<Message<VertxMessage>> {

		AtomicLong counter = new AtomicLong();

		CompletableFuture<Void> future;
		long limit;

		@Override
		public void handle(Message<VertxMessage> event) {
			if (counter.addAndGet(event.body().delta) >= limit) {
				future.complete(null);
			}
		}

	}

	protected static class AkkaMessage {

		public final int delta;

		public AkkaMessage(int delta) {
			this.delta = delta;
		}

	}

	protected static class AkkaListener extends AbstractActor {

		@Override
		public Receive createReceive() {
			return receiveBuilder().match(AkkaMessage.class, msg -> {
				if (akkaCounter.addAndGet(msg.delta) >= akkaLimit) {
					akkaPromise.complete(akkaCounter.get());
				}
			}).build();
		}

	}

}