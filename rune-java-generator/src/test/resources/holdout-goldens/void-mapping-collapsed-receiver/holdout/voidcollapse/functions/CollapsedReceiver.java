package holdout.voidcollapse.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import holdout.voidcollapse.Carrier;


@ImplementedBy(CollapsedReceiver.CollapsedReceiverDefault.class)
public abstract class CollapsedReceiver implements RosettaFunction {

	/**
	* @param flag 
	* @param c1 
	* @param c2 
	* @return r 
	*/
	public Void evaluate(Boolean flag, Carrier c1, Carrier c2) {
		Void r = doEvaluate(flag, c1, c2);
		
		return r;
	}

	protected abstract Void doEvaluate(Boolean flag, Carrier c1, Carrier c2);

	public static class CollapsedReceiverDefault extends CollapsedReceiver {
		@Override
		protected Void doEvaluate(Boolean flag, Carrier c1, Carrier c2) {
			Void r = null;
			return assignOutput(r, flag, c1, c2);
		}
		
		protected Void assignOutput(Void r, Boolean flag, Carrier c1, Carrier c2) {
			r = null;
			
			return r;
		}
	}
}
