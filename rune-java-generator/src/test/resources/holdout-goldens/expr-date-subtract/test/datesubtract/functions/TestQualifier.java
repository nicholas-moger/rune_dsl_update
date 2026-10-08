package test.datesubtract.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.expression.MapperMaths;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.records.Date;
import test.datesubtract.Test;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(TestQualifier.TestQualifierDefault.class)
public abstract class TestQualifier implements RosettaFunction {

	/**
	* @param test 
	* @return result 
	*/
	public Boolean evaluate(Test test) {
		Boolean result = doEvaluate(test);
		
		return result;
	}

	protected abstract Boolean doEvaluate(Test test);

	public static class TestQualifierDefault extends TestQualifier {
		@Override
		protected Boolean doEvaluate(Test test) {
			Boolean result = null;
			return assignOutput(result, test);
		}
		
		protected Boolean assignOutput(Boolean result, Test test) {
			result = areEqual(MapperMaths.<Integer, Date, Date>subtract(MapperS.of(test).<Date>map("getOne", _test -> _test.getOne()), MapperS.of(test).<Date>map("getTwo", _test -> _test.getTwo())), MapperS.of(42), CardinalityOperator.All).get();
			
			return result;
		}
	}
}
