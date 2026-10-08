package test.expressions.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(SwitchExample.SwitchExampleDefault.class)
public abstract class SwitchExample implements RosettaFunction {

	/**
	* @param x 
	* @return result 
	*/
	public BigDecimal evaluate(BigDecimal x) {
		BigDecimal result = doEvaluate(x);
		
		return result;
	}

	protected abstract BigDecimal doEvaluate(BigDecimal x);

	public static class SwitchExampleDefault extends SwitchExample {
		@Override
		protected BigDecimal doEvaluate(BigDecimal x) {
			BigDecimal result = null;
			return assignOutput(result, x);
		}
		
		protected BigDecimal assignOutput(BigDecimal result, BigDecimal x) {
			final MapperS<BigDecimal> switchArgument = MapperS.of(x);
			if (switchArgument.get() == null) {
				result = null;
			} else if (areEqual(switchArgument, MapperS.of(BigDecimal.valueOf(1)), CardinalityOperator.All).get()) {
				result = BigDecimal.valueOf(10);
			} else if (areEqual(switchArgument, MapperS.of(BigDecimal.valueOf(2)), CardinalityOperator.All).get()) {
				result = BigDecimal.valueOf(20);
			} else {
				result = BigDecimal.valueOf(0);
			}
			
			return result;
		}
	}
}
