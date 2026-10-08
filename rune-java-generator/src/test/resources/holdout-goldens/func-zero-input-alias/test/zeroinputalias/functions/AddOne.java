package test.zeroinputalias.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;


@ImplementedBy(AddOne.AddOneDefault.class)
public abstract class AddOne implements RosettaFunction {

	/**
	* @param arg 
	* @return out 
	*/
	public Integer evaluate(Integer arg) {
		Integer out = doEvaluate(arg);
		
		return out;
	}

	protected abstract Integer doEvaluate(Integer arg);

	public static class AddOneDefault extends AddOne {
		@Override
		protected Integer doEvaluate(Integer arg) {
			Integer out = null;
			return assignOutput(out, arg);
		}
		
		protected Integer assignOutput(Integer out, Integer arg) {
			return out;
		}
	}
}
