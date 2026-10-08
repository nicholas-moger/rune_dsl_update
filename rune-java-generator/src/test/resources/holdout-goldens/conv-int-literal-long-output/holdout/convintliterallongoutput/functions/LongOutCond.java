package holdout.convintliterallongoutput.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;


@ImplementedBy(LongOutCond.LongOutCondDefault.class)
public abstract class LongOutCond implements RosettaFunction {

	/**
	* @param f 
	* @return r 
	*/
	public Long evaluate(Boolean f) {
		Long r = doEvaluate(f);
		
		return r;
	}

	protected abstract Long doEvaluate(Boolean f);

	public static class LongOutCondDefault extends LongOutCond {
		@Override
		protected Long doEvaluate(Boolean f) {
			Long r = null;
			return assignOutput(r, f);
		}
		
		protected Long assignOutput(Long r, Boolean f) {
			if ((f == null ? false : f)) {
				r = 123456789012l;
			} else {
				r = (long) 42;
			}
			
			return r;
		}
	}
}
