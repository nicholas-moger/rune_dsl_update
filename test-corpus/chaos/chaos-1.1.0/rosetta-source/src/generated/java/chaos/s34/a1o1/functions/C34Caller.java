package chaos.s34.a1o1.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import javax.inject.Inject;


@ImplementedBy(C34Caller.C34CallerDefault.class)
public abstract class C34Caller implements RosettaFunction {
	
	// RosettaFunction dependencies
	//
	@Inject protected C34Impl c34Impl;

	/**
	* @param s 
	* @return t 
	*/
	public String evaluate(String s) {
		String t = doEvaluate(s);
		
		return t;
	}

	protected abstract String doEvaluate(String s);

	public static class C34CallerDefault extends C34Caller {
		@Override
		protected String doEvaluate(String s) {
			String t = null;
			return assignOutput(t, s);
		}
		
		protected String assignOutput(String t, String s) {
			t = c34Impl.evaluate(s);
			
			return t;
		}
	}
}
