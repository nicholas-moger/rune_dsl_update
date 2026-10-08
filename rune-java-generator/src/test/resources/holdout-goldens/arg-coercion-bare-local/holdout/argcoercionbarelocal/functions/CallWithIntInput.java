package holdout.argcoercionbarelocal.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import java.math.BigDecimal;
import javax.inject.Inject;


@ImplementedBy(CallWithIntInput.CallWithIntInputDefault.class)
public abstract class CallWithIntInput implements RosettaFunction {
	
	// RosettaFunction dependencies
	//
	@Inject protected Check check;

	/**
	* @param n 
	* @return ok 
	*/
	public Boolean evaluate(Integer n) {
		Boolean ok = doEvaluate(n);
		
		return ok;
	}

	protected abstract Boolean doEvaluate(Integer n);

	public static class CallWithIntInputDefault extends CallWithIntInput {
		@Override
		protected Boolean doEvaluate(Integer n) {
			Boolean ok = null;
			return assignOutput(ok, n);
		}
		
		protected Boolean assignOutput(Boolean ok, Integer n) {
			ok = check.evaluate((n == null ? null : BigDecimal.valueOf(n)));
			
			return ok;
		}
	}
}
