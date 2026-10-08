package test.chswitch.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;


@ImplementedBy(Fmt.FmtDefault.class)
public abstract class Fmt implements RosettaFunction {

	/**
	* @param s 
	* @return out 
	*/
	public String evaluate(String s) {
		String out = doEvaluate(s);
		
		return out;
	}

	protected abstract String doEvaluate(String s);

	public static class FmtDefault extends Fmt {
		@Override
		protected String doEvaluate(String s) {
			String out = null;
			return assignOutput(out, s);
		}
		
		protected String assignOutput(String out, String s) {
			out = s;
			
			return out;
		}
	}
}
