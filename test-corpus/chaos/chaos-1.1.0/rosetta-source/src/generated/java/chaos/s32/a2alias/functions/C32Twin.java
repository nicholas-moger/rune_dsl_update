package chaos.s32.a2alias.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;


@ImplementedBy(C32Twin.C32TwinDefault.class)
public abstract class C32Twin implements RosettaFunction {

	/**
	* @param s 
	* @return t 
	*/
	public String evaluate(String s) {
		String t = doEvaluate(s);
		
		return t;
	}

	protected abstract String doEvaluate(String s);

	public static class C32TwinDefault extends C32Twin {
		@Override
		protected String doEvaluate(String s) {
			String t = null;
			return assignOutput(t, s);
		}
		
		protected String assignOutput(String t, String s) {
			t = s;
			
			return t;
		}
	}
}
