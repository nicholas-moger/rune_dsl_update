package test.fsingle041.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;


@ImplementedBy(Test.TestDefault.class)
public abstract class Test implements RosettaFunction {

	/**
	* @param inp 
	* @return result 
	*/
	public Integer evaluate(Boolean inp) {
		Integer result = doEvaluate(inp);
		
		return result;
	}

	protected abstract Integer doEvaluate(Boolean inp);

	public static class TestDefault extends Test {
		@Override
		protected Integer doEvaluate(Boolean inp) {
			Integer result = null;
			return assignOutput(result, inp);
		}
		
		protected Integer assignOutput(Integer result, Boolean inp) {
			result = MapperS.of(42)
				.filterSingleNullSafe(item -> inp).get();
			
			return result;
		}
	}
}
