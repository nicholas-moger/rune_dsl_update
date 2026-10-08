package test.foneof024.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.util.Arrays;
import test.foneof024.A;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(TestOnlyExists.TestOnlyExistsDefault.class)
public abstract class TestOnlyExists implements RosettaFunction {

	/**
	* @param a 
	* @return result 
	*/
	public Boolean evaluate(A a) {
		Boolean result = doEvaluate(a);
		
		return result;
	}

	protected abstract Boolean doEvaluate(A a);

	public static class TestOnlyExistsDefault extends TestOnlyExists {
		@Override
		protected Boolean doEvaluate(A a) {
			Boolean result = null;
			return assignOutput(result, a);
		}
		
		protected Boolean assignOutput(Boolean result, A a) {
			result = onlyExists(MapperS.of(a), Arrays.asList("a1", "a2", "a3"), Arrays.asList("a1")).get();
			
			return result;
		}
	}
}
