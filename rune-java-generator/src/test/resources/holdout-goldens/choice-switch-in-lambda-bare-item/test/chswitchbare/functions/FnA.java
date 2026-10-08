package test.chswitchbare.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import test.chswitchbare.OptA;


@ImplementedBy(FnA.FnADefault.class)
public abstract class FnA implements RosettaFunction {

	/**
	* @param a 
	* @return r 
	*/
	public String evaluate(OptA a) {
		String r = doEvaluate(a);
		
		return r;
	}

	protected abstract String doEvaluate(OptA a);

	public static class FnADefault extends FnA {
		@Override
		protected String doEvaluate(OptA a) {
			String r = null;
			return assignOutput(r, a);
		}
		
		protected String assignOutput(String r, OptA a) {
			r = MapperS.of(a).<String>map("getFieldA", optA -> optA.getFieldA()).get();
			
			return r;
		}
	}
}
