package test.fiflit147.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;


@ImplementedBy(A.ADefault.class)
public abstract class A implements RosettaFunction {

	/**
	* @param a 
	* @return result 
	*/
	public Boolean evaluate(Boolean a) {
		Boolean result = doEvaluate(a);
		
		return result;
	}

	protected abstract Boolean doEvaluate(Boolean a);

	public static class ADefault extends A {
		@Override
		protected Boolean doEvaluate(Boolean a) {
			Boolean result = null;
			return assignOutput(result, a);
		}
		
		protected Boolean assignOutput(Boolean result, Boolean a) {
			return result;
		}
	}
}
