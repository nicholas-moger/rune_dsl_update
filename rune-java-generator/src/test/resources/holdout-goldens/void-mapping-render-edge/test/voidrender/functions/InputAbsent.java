package test.voidrender.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(InputAbsent.InputAbsentDefault.class)
public abstract class InputAbsent implements RosettaFunction {

	/**
	* @param t 
	* @return r 
	*/
	public Boolean evaluate(Void t) {
		Boolean r = doEvaluate(t);
		
		return r;
	}

	protected abstract Boolean doEvaluate(Void t);

	public static class InputAbsentDefault extends InputAbsent {
		@Override
		protected Boolean doEvaluate(Void t) {
			Boolean r = null;
			return assignOutput(r, t);
		}
		
		protected Boolean assignOutput(Boolean r, Void t) {
			r = notExists(MapperS.<Void>ofNull()).get();
			
			return r;
		}
	}
}
