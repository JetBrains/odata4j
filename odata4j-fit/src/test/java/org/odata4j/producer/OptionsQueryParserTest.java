package org.odata4j.producer;

import org.junit.Test;
import org.odata4j.producer.resources.OptionsQueryParser;

/**
 * Created by Eugene Petrenko (eugene.petrenko@gmail.com)
 * Date: 05.01.12 13:27
 */
public class OptionsQueryParserTest {

  @Test
  public void testParsesFilter_PropertyCall() {
    OptionsQueryParser.parseFilter("PropertyName");
  }

  @Test
  public void testParsesFilter_PPropertyCall() {
    OptionsQueryParser.parseFilter("(PropertyName)");
  }

  @Test
  public void testParsesFilter_PBool() {
    OptionsQueryParser.parseFilter("(True)");
  }

  @Test
  public void testParsesFilter_PBool2() {
    OptionsQueryParser.parseFilter("(False)");
  }

  @Test
  public void testParsesFilter_CallExpression() {
    OptionsQueryParser.parseFilter("startswith(tolower(Id),'eas')");
  }

  @Test
  public void testParsesFilter_CallExpression2() {
    OptionsQueryParser.parseFilter("tolower(Id) eq 'jsonfx'");
  }

  @Test
  public void testParsesFilter_AndOr() {
    OptionsQueryParser.parseFilter("Property and startswith(tolower(Id),'eas')");
  }
}
