package chaos.s23.a1o4.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;


@ImplementedBy(C23Flag.C23FlagDefault.class)
public abstract class C23Flag implements RosettaFunction {

	/**
	* @param flag 
	* @param a 
	* @param b 
	* @return r 
	*/
	public String evaluate(Boolean flag, String a, String b) {
		String r = doEvaluate(flag, a, b);
		
		return r;
	}

	protected abstract String doEvaluate(Boolean flag, String a, String b);

	public static class C23FlagDefault extends C23Flag {
		@Override
		protected String doEvaluate(Boolean flag, String a, String b) {
			String r = null;
			return assignOutput(r, flag, a, b);
		}
		
		protected String assignOutput(String r, Boolean flag, String a, String b) {
			if ((flag == null ? false : flag)) {
				r = a;
			} else {
				r = b;
			}
			
			return r;
		}
	}
}
