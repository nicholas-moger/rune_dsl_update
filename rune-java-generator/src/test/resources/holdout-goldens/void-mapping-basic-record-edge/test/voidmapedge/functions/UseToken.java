package test.voidmapedge.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;


@ImplementedBy(UseToken.UseTokenDefault.class)
public abstract class UseToken implements RosettaFunction {

	/**
	* @param t 
	* @return r 
	*/
	public Void evaluate(Void t) {
		Void r = doEvaluate(t);
		
		return r;
	}

	protected abstract Void doEvaluate(Void t);

	public static class UseTokenDefault extends UseToken {
		@Override
		protected Void doEvaluate(Void t) {
			Void r = null;
			return assignOutput(r, t);
		}
		
		protected Void assignOutput(Void r, Void t) {
			r = null;
			
			return r;
		}
	}
}
