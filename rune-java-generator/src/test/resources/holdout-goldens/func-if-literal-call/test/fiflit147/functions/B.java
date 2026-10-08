package test.fiflit147.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import javax.inject.Inject;


@ImplementedBy(B.BDefault.class)
public abstract class B implements RosettaFunction {
	
	// RosettaFunction dependencies
	//
	@Inject protected A a;

	/**
	* @return result 
	*/
	public Boolean evaluate() {
		Boolean result = doEvaluate();
		
		return result;
	}

	protected abstract Boolean doEvaluate();

	public static class BDefault extends B {
		@Override
		protected Boolean doEvaluate() {
			Boolean result = null;
			return assignOutput(result);
		}
		
		protected Boolean assignOutput(Boolean result) {
			final Boolean ifThenElseResult;
			if (true) {
				ifThenElseResult = true;
			} else if (false) {
				ifThenElseResult = true;
			} else {
				ifThenElseResult = null;
			}
			result = a.evaluate(ifThenElseResult);
			
			return result;
		}
	}
}
