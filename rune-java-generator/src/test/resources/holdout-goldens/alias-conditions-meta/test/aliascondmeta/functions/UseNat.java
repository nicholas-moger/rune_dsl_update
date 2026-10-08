package test.aliascondmeta.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;


@ImplementedBy(UseNat.UseNatDefault.class)
public abstract class UseNat implements RosettaFunction {

	/**
	* @param v 
	* @return r 
	*/
	public Integer evaluate(Integer v) {
		Integer r = doEvaluate(v);
		
		return r;
	}

	protected abstract Integer doEvaluate(Integer v);

	public static class UseNatDefault extends UseNat {
		@Override
		protected Integer doEvaluate(Integer v) {
			Integer r = null;
			return assignOutput(r, v);
		}
		
		protected Integer assignOutput(Integer r, Integer v) {
			r = v;
			
			return r;
		}
	}
}
