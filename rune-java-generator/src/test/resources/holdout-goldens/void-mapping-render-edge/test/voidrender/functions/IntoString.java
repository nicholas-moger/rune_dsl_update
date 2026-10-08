package test.voidrender.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;


@ImplementedBy(IntoString.IntoStringDefault.class)
public abstract class IntoString implements RosettaFunction {

	/**
	* @param t 
	* @return r 
	*/
	public String evaluate(Void t) {
		String r = doEvaluate(t);
		
		return r;
	}

	protected abstract String doEvaluate(Void t);

	public static class IntoStringDefault extends IntoString {
		@Override
		protected String doEvaluate(Void t) {
			String r = null;
			return assignOutput(r, t);
		}
		
		protected String assignOutput(String r, Void t) {
			r = null;
			
			return r;
		}
	}
}
