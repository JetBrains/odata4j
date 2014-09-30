package org.odata4j.stax2;

import org.odata4j.internal.PlatformUtil;
import java.util.logging.Logger;

public abstract class XMLFactoryProvider2 {
  public static final String XML_PULL_FACTORY = "org.odata4j.stax2.xppimpl.XmlPullXMLFactoryProvider2";
  public static final String XML_STAX_FACTORY = "org.odata4j.stax2.staximpl.StaxXMLFactoryProvider2";
  public static final String XML_DOM_FACTORY = "org.odata4j.stax2.domimpl.DomXMLFactoryProvider2";
  private static XMLFactoryProvider2 ourFactory;

  private static XMLFactoryProvider2 tryLoadClass(String clazz) {
    try {
      return (XMLFactoryProvider2) Class.forName(clazz).newInstance();
    } catch (Throwable t) {
      Logger.getLogger(XMLFactoryProvider2.class.getName()).fine("Failed to create instance of " + clazz + ". " + t.getMessage());
      return null;
    }
  }

  public static void setInstance(XMLFactoryProvider2 instance) {
    ourFactory = instance;
  }

  public static XMLFactoryProvider2 getInstance() {
    if (!PlatformUtil.runningOnAndroid()) {
      if (ourFactory == null) {
        ourFactory = tryLoadClass(XML_DOM_FACTORY);
      }
      if (ourFactory == null) {
        ourFactory = tryLoadClass(XML_STAX_FACTORY);
      }
    }

    if (ourFactory == null) {
      ourFactory = tryLoadClass(XML_PULL_FACTORY);
    }

    if (ourFactory == null) {
      throw new RuntimeException("Failed to load XmlFactoryProvider");
    }
    return ourFactory;
  }

  public abstract XMLOutputFactory2 newXMLOutputFactory2();

  public abstract XMLInputFactory2 newXMLInputFactory2();

  public abstract XMLWriterFactory2 newXMLWriterFactory2();

}
