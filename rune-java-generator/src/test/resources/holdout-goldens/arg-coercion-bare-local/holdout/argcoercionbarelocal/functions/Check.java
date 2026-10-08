package holdout.argcoercionbarelocal.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(Check.CheckDefault.class)
public abstract class Check implements RosettaFunction {

	/**
	* @param v 
	* @return ok 
	*/
	public Boolean evaluate(BigDecimal v) {
		Boolean ok = doEvaluate(v);
		
		return ok;
	}

	protected abstract Boolean doEvaluate(BigDecimal v);

	public static class CheckDefault extends Check {
		@Override
		protected Boolean doEvaluate(BigDecimal v) {
			Boolean ok = null;
			return assignOutput(ok, v);
		}
		
		protected Boolean assignOutput(Boolean ok, BigDecimal v) {
			ok = greaterThanEquals(MapperS.of(v), MapperS.of(BigDecimal.valueOf(0)), CardinalityOperator.All).get();
			
			return ok;
		}
	}
}
