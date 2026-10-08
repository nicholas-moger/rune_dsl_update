package test.voidhoist.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;


@ImplementedBy(SetConditional.SetConditionalDefault.class)
public abstract class SetConditional implements RosettaFunction {

	/**
	* @param flag 
	* @param t 
	* @param u 
	* @return r 
	*/
	public Void evaluate(Boolean flag, Void t, Void u) {
		Void r = doEvaluate(flag, t, u);
		
		return r;
	}

	protected abstract Void doEvaluate(Boolean flag, Void t, Void u);

	public static class SetConditionalDefault extends SetConditional {
		@Override
		protected Void doEvaluate(Boolean flag, Void t, Void u) {
			Void r = null;
			return assignOutput(r, flag, t, u);
		}
		
		protected Void assignOutput(Void r, Boolean flag, Void t, Void u) {
			r = null;
			
			return r;
		}
	}
}
