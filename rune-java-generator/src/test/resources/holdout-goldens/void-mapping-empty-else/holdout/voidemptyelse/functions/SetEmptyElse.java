package holdout.voidemptyelse.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;


@ImplementedBy(SetEmptyElse.SetEmptyElseDefault.class)
public abstract class SetEmptyElse implements RosettaFunction {

	/**
	* @param flag 
	* @param t 
	* @return r 
	*/
	public Void evaluate(Boolean flag, Void t) {
		Void r = doEvaluate(flag, t);
		
		return r;
	}

	protected abstract Void doEvaluate(Boolean flag, Void t);

	public static class SetEmptyElseDefault extends SetEmptyElse {
		@Override
		protected Void doEvaluate(Boolean flag, Void t) {
			Void r = null;
			return assignOutput(r, flag, t);
		}
		
		protected Void assignOutput(Void r, Boolean flag, Void t) {
			r = null;
			
			return r;
		}
	}
}
