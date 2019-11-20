package services.moleculer.speedtest;

import java.nio.charset.StandardCharsets;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import junit.framework.TestCase;
import services.moleculer.ServiceBroker;
import services.moleculer.service.Action;
import services.moleculer.service.Service;
import services.moleculer.util.CommonUtils;
import services.moleculer.web.ApiGateway;
import services.moleculer.web.netty.NettyServer;
import services.moleculer.web.router.Route;

@SpringBootApplication
public class RestTest extends TestCase {

	// --- VARIABLES ---

	protected ConfigurableApplicationContext springContext;
	protected CloseableHttpAsyncClient client;
	
	protected ServiceBroker broker;

	// --- SET UP ---

	@Override
	protected void setUp() throws Exception {

		// Init Spring Context
		System.setProperty("logging.config", "classpath:logging-development.properties");
		springContext = SpringApplication.run(RestTest.class, new String[0]);

		// Init ServiceBroker
		broker = new ServiceBroker();
		broker.createService(new NettyServer(3000));
		ApiGateway gateway = new ApiGateway();
		Route route = gateway.addRoute(new Route());
		route.addAlias("/add/:a/:b", "math.add");
		broker.createService(gateway);
		broker.createService(new Service("math") {
			
			@SuppressWarnings("unused")
			Action add = ctx -> {
				int a = ctx.params.get("a", 0);
				int b = ctx.params.get("b", 0);
				ctx.params.put("c", a + b);
				return ctx.params;
			};
			
		});
		broker.start();
		
		// Init HTTP client
		client = HttpAsyncClients.createDefault();
		client.start();
	}

	// --- DO TESTS ---

	@Test
	public void testRest() throws Exception {

		// Spring test
		long duration = doTest(8080, 10000);
		System.out.println(duration);
		
		// Moleculer test
		duration = doTest(3000, 10000);
		System.out.println(duration);
	}

	protected long doTest(int port, int loops) throws Exception {
		HttpGet get = new HttpGet("http://localhost:" + port + "/add/3/4");
		long start = System.currentTimeMillis();
		for (int i = 0; i < loops; i++) {

			// Check status
			HttpResponse rsp = client.execute(get, null).get();
			assertEquals(200, rsp.getStatusLine().getStatusCode());
						
			// {"a":3,"b":4,"c":7}
			byte[] bytes = CommonUtils.readFully(rsp.getEntity().getContent());			
			String txt = new String(bytes, StandardCharsets.UTF_8);
			assertTrue(txt.endsWith(":7}"));
		}
		return System.currentTimeMillis() - start;
	}
	
	// --- TEAR DOWN ---

	@Override
	protected void tearDown() throws Exception {

		// Stop Moleculer
		if (broker != null) {
			broker.stop();
		}
		
		// Stop Spring Context
		if (springContext != null) {
			springContext.stop();
		}

		// Stop HTTP client
		if (client != null) {
			client.close();
		}
	}

}