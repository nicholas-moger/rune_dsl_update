package holdout.voidhoistsecond.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(ExistsThenConditional.ExistsThenConditionalDefault.class)
public abstract class ExistsThenConditional implements RosettaFunction {

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

	public static class ExistsThenConditionalDefault extends ExistsThenConditional {
		@Override
		protected Boolean doEvaluate(Boolean flag, Void t, Void u) {
			Boolean r = null;
			return assignOutput(r, flag, t, u);
		}
		
		protected Boolean assignOutput(Boolean r, Boolean flag, Void t, Void u) {
			final MapperS<String> ifThenElseResult;
			if ((flag == null ? false : flag)) {
				ifThenElseResult = MapperS.of("a");
			} else {
				ifThenElseResult = MapperS.of("b");
			}
			r = exists(MapperS.<Void>ofNull()).andNullSafe(areEqual(ifThenElseResult, MapperS.of("a"), CardinalityOperator.All)).get();
			
			return r;
		}
	}
}
