package test.expressions.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;


@ImplementedBy(Logical.LogicalDefault.class)
public abstract class Logical implements RosettaFunction {

	/**
	* @param a 
	* @param b 
	* @param c 
	* @return result 
	*/
	public Boolean evaluate(Boolean a, Boolean b, Boolean c) {
		Boolean result = doEvaluate(a, b, c);
		
		return result;
	}

	protected abstract Boolean doEvaluate(Boolean a, Boolean b, Boolean c);

	public static class LogicalDefault extends Logical {
		@Override
		protected Boolean doEvaluate(Boolean a, Boolean b, Boolean c) {
			Boolean result = null;
			return assignOutput(result, a, b, c);
		}
		
		protected Boolean assignOutput(Boolean result, Boolean a, Boolean b, Boolean c) {
			result = ComparisonResult.ofNullSafe(MapperS.of(a)).orNullSafe(ComparisonResult.ofNullSafe(MapperS.of(b)).andNullSafe(ComparisonResult.ofNullSafe(MapperS.of(c)))).get();
			
			return result;
		}
	}
}
