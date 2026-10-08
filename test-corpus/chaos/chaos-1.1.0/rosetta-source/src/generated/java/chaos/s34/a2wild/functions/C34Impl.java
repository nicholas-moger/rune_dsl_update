package chaos.s34.a2wild.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;


@ImplementedBy(C34Impl.C34ImplDefault.class)
public abstract class C34Impl implements RosettaFunction {

	/**
	* @param s 
	* @return t 
	*/
	public String evaluate(String s) {
		String t = doEvaluate(s);
		
		return t;
	}

	protected abstract String doEvaluate(String s);

	public static class C34ImplDefault extends C34Impl {
		@Override
		protected String doEvaluate(String s) {
			String t = null;
			return assignOutput(t, s);
		}
		
		protected String assignOutput(String t, String s) {
			return t;
		}
	}
}
