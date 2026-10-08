package chaos.s18.a3third.p1.functions;

import chaos.s18.a3third.p1.C18KindEnum;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;


@ImplementedBy(C18Colour.C18ColourDefault.class)
public abstract class C18Colour implements RosettaFunction {

	/**
	* @param k 
	* @return hex 
	*/
	public String evaluate(C18KindEnum k) {
		String hex = doEvaluate(k);
		
		return hex;
	}

	protected abstract String doEvaluate(C18KindEnum k);

	public static class C18ColourDefault extends C18Colour {
		@Override
		protected String doEvaluate(C18KindEnum k) {
			String hex = null;
			return assignOutput(hex, k);
		}
		
		protected String assignOutput(String hex, C18KindEnum k) {
			if (k == null) {
				hex = null;
			} else if (k == C18KindEnum.RED) {
				hex = "ff0000";
			} else if (k == C18KindEnum.GREEN) {
				hex = "00ff00";
			} else {
				hex = "0000ff";
			}
			
			return hex;
		}
	}
}
