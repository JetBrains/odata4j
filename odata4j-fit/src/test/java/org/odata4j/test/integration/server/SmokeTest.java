package org.odata4j.test.integration.server;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpExchange;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Test;
import org.odata4j.producer.resources.DefaultODataProducerProvider;
import org.odata4j.test.integration.AbstractJettyHttpClientTest;
import org.odata4j.test.integration.TestInMemoryProducers;

public class SmokeTest extends AbstractJettyHttpClientTest {

  private static String META_DATA_URL;
  private static String FEED_URL;

  public SmokeTest(RuntimeFacadeType type) {
    super(type);
  }

  @Override
  public void setup() throws Exception {
    super.setup();

    META_DATA_URL = BASE_URI + "$metadata";
    FEED_URL = BASE_URI + TestInMemoryProducers.SIMPLE_ENTITY_SET_NAME;
  }

  @Override
  protected void registerODataProducer() throws Exception {
    DefaultODataProducerProvider.setInstance(TestInMemoryProducers.simple());
  }

  @Test
  public void serviceUrlReturnsOkStatus() throws Exception {
    ContentExchange exchange = sendRequest(BASE_URI);
    exchange.waitForDone();
    verifyOkStatusIsReturned(exchange);
  }

  @Test
  public void metaDataUrlReturnsOkStatus() throws Exception {
    ContentExchange exchange = sendRequest(META_DATA_URL);
    exchange.waitForDone();
    verifyOkStatusIsReturned(exchange);
  }

  @Test
  public void feedUrlReturnsOkStatus() throws Exception {
    ContentExchange exchange = sendRequest(FEED_URL);
    exchange.waitForDone();
    verifyOkStatusIsReturned(exchange);
  }

  private void verifyOkStatusIsReturned(ContentExchange exchange) throws Exception {
    assertThat(exchange.getStatus(), is(HttpExchange.STATUS_COMPLETED));
    assertThat(exchange.getResponseStatus(), is(HttpStatus.OK_200));
    assertThat(exchange.getResponseContent().length(), greaterThan(0));
  }
}
