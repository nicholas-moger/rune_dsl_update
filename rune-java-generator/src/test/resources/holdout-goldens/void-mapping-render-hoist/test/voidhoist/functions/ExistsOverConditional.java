package test.voidhoist.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(ExistsOverConditional.ExistsOverConditionalDefault.class)
public abstract class ExistsOverConditional implements RosettaFunction {

	/**
	* @param flag 
	* @param t 
	* @param u 
	* @return r 
	*/
	public Boolean evaluate(Boolean flag, Void t, Void u) {
		Boolean r = doEvaluate(flag, t, u);
		
		return r;
	}

	protected abstract Boolean doEvaluate(Boolean flag, Void t, Void u);

	public static class ExistsOverConditionalDefault extends ExistsOverConditional {
		@Override
		protected Boolean doEvaluate(Boolean flag, Void t, Void u) {
			Boolean r = null;
			return assignOutput(r, flag, t, u);
		}
		
		protected Boolean assignOutput(Boolean r, Boolean flag, Void t, Void u) {
			r = exists(MapperS.<Void>ofNull()).get();
			
			return r;
		}
	}
}
