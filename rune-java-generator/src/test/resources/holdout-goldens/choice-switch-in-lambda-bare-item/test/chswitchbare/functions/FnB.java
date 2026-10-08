package test.chswitchbare.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import test.chswitchbare.OptB;


@ImplementedBy(FnB.FnBDefault.class)
public abstract class FnB implements RosettaFunction {

	/**
	* @param b 
	* @return r 
	*/
	public String evaluate(OptB b) {
		String r = doEvaluate(b);
		
		return r;
	}

	protected abstract String doEvaluate(OptB b);

	public static class FnBDefault extends FnB {
		@Override
		protected String doEvaluate(OptB b) {
			String r = null;
			return assignOutput(r, b);
		}
		
		protected String assignOutput(String r, OptB b) {
			r = MapperS.of(b).<String>map("getFieldB", optB -> optB.getFieldB()).get();
			
			return r;
		}
	}
}
