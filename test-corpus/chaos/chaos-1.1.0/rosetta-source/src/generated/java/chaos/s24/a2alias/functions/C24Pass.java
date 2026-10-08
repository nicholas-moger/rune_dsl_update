package chaos.s24.a2alias.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;


@ImplementedBy(C24Pass.C24PassDefault.class)
public abstract class C24Pass implements RosettaFunction {

	/**
	* @param t 
	* @return u 
	*/
	public Void evaluate(Void t) {
		Void u = doEvaluate(t);
		
		return u;
	}

	protected abstract Void doEvaluate(Void t);

	public static class C24PassDefault extends C24Pass {
		@Override
		protected Void doEvaluate(Void t) {
			Void u = null;
			return assignOutput(u, t);
		}
		
		protected Void assignOutput(Void u, Void t) {
			u = null;
			
			return u;
		}
	}
}
