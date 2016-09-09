package org.odata4j.test.integration;

import org.junit.After;
import org.junit.Before;
import org.odata4j.producer.server.ODataServer;

/**
 * Base integration test class that:
 * <ol><li>starts an ODataServer,</li>
 * <li>registers an ODataProducer,</li>
 * <li>and starts a client</li></ol>
 */
public abstract class AbstractIntegrationTest extends AbstractRuntimeTest {

  protected static String BASE_URI;

  /**
   * The ODataServer instance.
   */
  protected ODataServer server;

  public AbstractIntegrationTest(RuntimeFacadeType type) {
    super(type);
  }

  @Before
  public void setup() throws Exception {
    BASE_URI = UriProvider.getEndpointUri("/test.svc/");
    startODataServer();
    registerODataProducer();
    startClient();
  }

  @After
  public void teardown() throws Exception {
    stopClient();
    stopODataServer();
  }

  protected void startODataServer() throws Exception {
    server = rtFacade.startODataServer(BASE_URI);
  }

  protected abstract void registerODataProducer() throws Exception;

  protected abstract void startClient() throws Exception;

  protected abstract void stopClient() throws Exception;

  protected void stopODataServer() throws Exception {
    server.stop();
  }
}
