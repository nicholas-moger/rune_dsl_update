package chaos.s26.base.functions;

import chaos.s26.base.C26KindEnum;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;


@ImplementedBy(C26Name.C26NameDefault.class)
public abstract class C26Name implements RosettaFunction {

	/**
	* @param k 
	* @return s 
	*/
	public String evaluate(C26KindEnum k) {
		String s = doEvaluate(k);
		
		return s;
	}

	protected abstract String doEvaluate(C26KindEnum k);

	public static class C26NameDefault extends C26Name {
		@Override
		protected String doEvaluate(C26KindEnum k) {
			String s = null;
			return assignOutput(s, k);
		}
		
		protected String assignOutput(String s, C26KindEnum k) {
			s = MapperS.of(k).map("to-string", C26KindEnum::toDisplayString).get();
			
			return s;
		}
	}
}
