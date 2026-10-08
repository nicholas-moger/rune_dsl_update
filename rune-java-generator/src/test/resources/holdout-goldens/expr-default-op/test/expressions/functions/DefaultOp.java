package test.expressions.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;


@ImplementedBy(DefaultOp.DefaultOpDefault.class)
public abstract class DefaultOp implements RosettaFunction {

	/**
	* @param x 
	* @return result 
	*/
	public BigDecimal evaluate(BigDecimal x) {
		BigDecimal result = doEvaluate(x);
		
		return result;
	}

	protected abstract BigDecimal doEvaluate(BigDecimal x);

	public static class DefaultOpDefault extends DefaultOp {
		@Override
		protected BigDecimal doEvaluate(BigDecimal x) {
			BigDecimal result = null;
			return assignOutput(result, x);
		}
		
		protected BigDecimal assignOutput(BigDecimal result, BigDecimal x) {
			result = MapperS.of(x).getOrDefault(BigDecimal.valueOf(0));
			
			return result;
		}
	}
}
