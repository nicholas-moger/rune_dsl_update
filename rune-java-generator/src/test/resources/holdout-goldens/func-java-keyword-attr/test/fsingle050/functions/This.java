package test.fsingle050.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;


@ImplementedBy(This.ThisDefault.class)
public abstract class This implements RosettaFunction {

	/**
	* @return _static 
	*/
	public Integer evaluate() {
		Integer _static = doEvaluate();
		
		return _static;
	}

	protected abstract Integer doEvaluate();

	public static class ThisDefault extends This {
		@Override
		protected Integer doEvaluate() {
			Integer _static = null;
			return assignOutput(_static);
		}
		
		protected Integer assignOutput(Integer _static) {
			_static = 42;
			
			return _static;
		}
	}
}
