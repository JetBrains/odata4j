package org.odata4j.format.xml;

import org.junit.Assert;
import org.junit.Test;
import org.odata4j.stax2.XMLFactoryProvider2;
import org.odata4j.stax2.XMLWriter2;
import org.odata4j.stax2.domimpl.DomXMLFactoryProvider2;

import java.io.StringWriter;

/**
 * Created by Eugene Petrenko (eugene.petrenko@gmail.com)
 * Date: 11.01.12 11:09
 */
public class XmlWriterTest {
  @Test
  public void testElementWithNoContent_DOM() {
    doElementWithNoContentTest(new DomXMLFactoryProvider2());
  }

  @Test
  public void testElementWithNoContent_Default() {
    XMLFactoryProvider2.setInstance(null);
    XMLFactoryProvider2 instance = XMLFactoryProvider2.getInstance();
    System.out.println("instance.getClass() = " + instance.getClass());
    doElementWithNoContentTest(instance);
  }

  @Test
  public void testDOMClass() throws ClassNotFoundException {
    Class.forName(XMLFactoryProvider2.XML_DOM_FACTORY);
  }

  private void doElementWithNoContentTest(XMLFactoryProvider2 factory) {
    StringWriter sw = new StringWriter();
    XMLWriter2 writer = factory.newXMLWriterFactory2().createXMLWriter(sw);
    writer.startDocument();
    writer.startElement("foo");
    writer.writeAttribute("aaa", "bbb");
    writer.writeAttribute("ccc", "ddd");
    writer.endElement("foo");
    writer.endDocument();

    final String text = sw.toString();
    System.out.println("text = " + text);

    Assert.assertFalse(text.contains("></foo"));
  }
}