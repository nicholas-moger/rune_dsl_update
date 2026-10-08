package holdout.convintliterallongoutput.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;


@ImplementedBy(LongOutIntInputCond.LongOutIntInputCondDefault.class)
public abstract class LongOutIntInputCond implements RosettaFunction {

	/**
	* @param f 
	* @param n 
	* @return r 
	*/
	public Long evaluate(Boolean f, Integer n) {
		Long r = doEvaluate(f, n);
		
		return r;
	}

	protected abstract Long doEvaluate(Boolean f, Integer n);

	public static class LongOutIntInputCondDefault extends LongOutIntInputCond {
		@Override
		protected Long doEvaluate(Boolean f, Integer n) {
			Long r = null;
			return assignOutput(r, f, n);
		}
		
		protected Long assignOutput(Long r, Boolean f, Integer n) {
			if ((f == null ? false : f)) {
				if (n == null) {
					r = null;
				} else {
					r = n.longValue();
				}
			} else {
				r = (long) 42;
			}
			
			return r;
		}
	}
}
