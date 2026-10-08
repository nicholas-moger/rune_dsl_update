package holdout.convintliterallongoutput.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;


@ImplementedBy(LongOutWhole.LongOutWholeDefault.class)
public abstract class LongOutWhole implements RosettaFunction {

	/**
	* @return r 
	*/
	public Long evaluate() {
		Long r = doEvaluate();
		
		return r;
	}

	protected abstract Long doEvaluate();

	public static class LongOutWholeDefault extends LongOutWhole {
		@Override
		protected Long doEvaluate() {
			Long r = null;
			return assignOutput(r);
		}
		
		protected Long assignOutput(Long r) {
			r = (long) 42;
			
			return r;
		}
	}
}
