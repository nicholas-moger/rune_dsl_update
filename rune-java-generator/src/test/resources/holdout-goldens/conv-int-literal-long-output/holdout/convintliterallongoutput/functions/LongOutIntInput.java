package holdout.convintliterallongoutput.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;


@ImplementedBy(LongOutIntInput.LongOutIntInputDefault.class)
public abstract class LongOutIntInput implements RosettaFunction {

	/**
	* @param n 
	* @return r 
	*/
	public Long evaluate(Integer n) {
		Long r = doEvaluate(n);
		
		return r;
	}

	protected abstract Long doEvaluate(Integer n);

	public static class LongOutIntInputDefault extends LongOutIntInput {
		@Override
		protected Long doEvaluate(Integer n) {
			Long r = null;
			return assignOutput(r, n);
		}
		
		protected Long assignOutput(Long r, Integer n) {
			if (n == null) {
				r = null;
			} else {
				r = n.longValue();
			}
			
			return r;
		}
	}
}
