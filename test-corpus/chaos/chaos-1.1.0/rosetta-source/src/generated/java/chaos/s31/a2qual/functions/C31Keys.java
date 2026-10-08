package chaos.s31.a2qual.functions;

import chaos.s31.a2qual.C31KeyEnum;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C31Keys.C31KeysDefault.class)
public abstract class C31Keys implements RosettaFunction {

	/**
	* @param k 
	* @param raw 
	* @return ok 
	*/
	public Boolean evaluate(C31KeyEnum k, String raw) {
		Boolean ok = doEvaluate(k, raw);
		
		return ok;
	}

	protected abstract Boolean doEvaluate(C31KeyEnum k, String raw);

	public static class C31KeysDefault extends C31Keys {
		@Override
		protected Boolean doEvaluate(C31KeyEnum k, String raw) {
			Boolean ok = null;
			return assignOutput(ok, k, raw);
		}
		
		protected Boolean assignOutput(Boolean ok, C31KeyEnum k, String raw) {
			final ComparisonResult ifThenElseResult;
			if (k == null) {
				ifThenElseResult = ComparisonResult.ofEmpty();
			} else if (k == C31KeyEnum.LONG) {
				ifThenElseResult = ComparisonResult.ofNullSafe(MapperS.of(true));
			} else if (k == C31KeyEnum.NEW) {
				ifThenElseResult = ComparisonResult.ofNullSafe(MapperS.of(false));
			} else {
				ifThenElseResult = areEqual(MapperS.of(raw).checkedMap("to-enum", C31KeyEnum::fromDisplayName, IllegalArgumentException.class), MapperS.of(k), CardinalityOperator.All);
			}
			ok = areEqual(MapperS.of(k), MapperS.of(C31KeyEnum.CLASS), CardinalityOperator.All).orNullSafe(ifThenElseResult).get();
			
			return ok;
		}
	}
}
