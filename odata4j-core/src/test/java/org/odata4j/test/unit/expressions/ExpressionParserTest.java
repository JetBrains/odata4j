package org.odata4j.test.unit.expressions;

import junit.framework.Assert;
import org.junit.Test;
import org.odata4j.expression.CommonExpression;
import org.odata4j.expression.ExpressionParser;
import org.odata4j.expression.PrintExpressionVisitor;

public class ExpressionParserTest {
    @Test
    public void testLiteralExpression() {
        CommonExpression expression = ExpressionParser.parse("'MyTestLibrary'");
        Assert.assertNotNull(expression);
        PrintExpressionVisitor visitor = new PrintExpressionVisitor();
        expression.visitThis(visitor);
        Assert.assertEquals("string(MyTestLibrary)", visitor.toString());
    }

    @Test
    public void testLiteralWithQuoteExpression() {
        CommonExpression expression = ExpressionParser.parse("''MyTestLibrary'");
        Assert.assertNotNull(expression);
        PrintExpressionVisitor visitor = new PrintExpressionVisitor();
        expression.visitThis(visitor);
        Assert.assertEquals("string('MyTestLibrary)", visitor.toString());
    }
}
