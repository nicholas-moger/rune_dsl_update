package chaos.s17.x26enum.functions;

import chaos.s17.x26enum.C17SideEnum;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;


@ImplementedBy(C17Which.C17WhichDefault.class)
public abstract class C17Which implements RosettaFunction {

	/**
	* @param s 
	* @return r 
	*/
	public String evaluate(C17SideEnum s) {
		String r = doEvaluate(s);
		
		return r;
	}

	protected abstract String doEvaluate(C17SideEnum s);

	public static class C17WhichDefault extends C17Which {
		@Override
		protected String doEvaluate(C17SideEnum s) {
			String r = null;
			return assignOutput(r, s);
		}
		
		protected String assignOutput(String r, C17SideEnum s) {
			if (s == null) {
				r = null;
			} else if (s == C17SideEnum.BUY) {
				r = "side-buy";
			} else if (s == C17SideEnum.SELL) {
				r = "side-sell";
			} else {
				r = null;
			}
			
			return r;
		}
	}
}
