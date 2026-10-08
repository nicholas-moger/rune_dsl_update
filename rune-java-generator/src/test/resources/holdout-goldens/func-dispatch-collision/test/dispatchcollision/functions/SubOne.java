package test.dispatchcollision.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;


@ImplementedBy(SubOne.SubOneDefault.class)
public abstract class SubOne implements RosettaFunction {

	/**
	* @param arg 
	* @return out 
	*/
	public String evaluate(String arg) {
		String out = doEvaluate(arg);
		
		return out;
	}

	protected abstract String doEvaluate(String arg);

	public static class SubOneDefault extends SubOne {
		@Override
		protected String doEvaluate(String arg) {
			String out = null;
			return assignOutput(out, arg);
		}
		
		protected String assignOutput(String out, String arg) {
			return out;
		}
	}
}
