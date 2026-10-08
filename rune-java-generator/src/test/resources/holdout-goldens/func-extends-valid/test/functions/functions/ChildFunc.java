package test.functions.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.MapperMaths;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;


@ImplementedBy(ChildFunc.ChildFuncDefault.class)
public abstract class ChildFunc implements RosettaFunction {

	/**
	* @param x 
	* @return result 
	*/
	public BigDecimal evaluate(BigDecimal x) {
		BigDecimal result = doEvaluate(x);
		
		return result;
	}

	protected abstract BigDecimal doEvaluate(BigDecimal x);

	public static class ChildFuncDefault extends ChildFunc {
		@Override
		protected BigDecimal doEvaluate(BigDecimal x) {
			BigDecimal result = null;
			return assignOutput(result, x);
		}
		
		protected BigDecimal assignOutput(BigDecimal result, BigDecimal x) {
			result = MapperMaths.<BigDecimal, BigDecimal, BigDecimal>add(MapperS.of(x), MapperS.of(BigDecimal.valueOf(1))).get();
			
			return result;
		}
	}
}
