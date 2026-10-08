package test.functions.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import java.math.BigDecimal;


@ImplementedBy(BaseFunc.BaseFuncDefault.class)
public abstract class BaseFunc implements RosettaFunction {

	/**
	* @param x 
	* @return result 
	*/
	public BigDecimal evaluate(BigDecimal x) {
		BigDecimal result = doEvaluate(x);
		
		return result;
	}

	protected abstract BigDecimal doEvaluate(BigDecimal x);

	public static class BaseFuncDefault extends BaseFunc {
		@Override
		protected BigDecimal doEvaluate(BigDecimal x) {
			BigDecimal result = null;
			return assignOutput(result, x);
		}
		
		protected BigDecimal assignOutput(BigDecimal result, BigDecimal x) {
			return result;
		}
	}
}
