package chaos.s18.a1o4.functions;

import chaos.s18.a1o4.C18KindEnum;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C18Maybe.C18MaybeDefault.class)
public abstract class C18Maybe implements RosettaFunction {

	/**
	* @param k 
	* @return hx 
	*/
	public String evaluate(C18KindEnum k) {
		String hx = doEvaluate(k);
		
		return hx;
	}

	protected abstract String doEvaluate(C18KindEnum k);

	public static class C18MaybeDefault extends C18Maybe {
		@Override
		protected String doEvaluate(C18KindEnum k) {
			String hx = null;
			return assignOutput(hx, k);
		}
		
		protected String assignOutput(String hx, C18KindEnum k) {
			if (exists(MapperS.of(k)).getOrDefault(false)) {
				if (k == null) {
					hx = null;
				} else if (k == C18KindEnum.RED) {
					hx = "ff0000";
				} else {
					hx = "xx";
				}
			} else {
				hx = null;
			}
			
			return hx;
		}
	}
}
