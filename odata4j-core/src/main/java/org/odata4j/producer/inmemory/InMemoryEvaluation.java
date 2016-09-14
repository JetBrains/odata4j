package org.odata4j.producer.inmemory;

import org.odata4j.expression.BoolCommonExpression;
import org.odata4j.expression.CommonExpression;

public interface InMemoryEvaluation {
    Object evaluate(CommonExpression expression, Object target, PropertyModel properties);

    boolean evaluate(BoolCommonExpression expression, Object target, PropertyModel properties);
}
