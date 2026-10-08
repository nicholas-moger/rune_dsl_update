package chaos.s21.a3hub.p2.functions;

import chaos.s21.a3hub.p2.C21Paths;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.util.Arrays;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C21Gate.C21GateDefault.class)
public abstract class C21Gate implements RosettaFunction {

	/**
	* @param t 
	* @return ok 
	*/
	public Boolean evaluate(C21Paths t) {
		Boolean ok = doEvaluate(t);
		
		return ok;
	}

	protected abstract Boolean doEvaluate(C21Paths t);

	public static class C21GateDefault extends C21Gate {
		@Override
		protected Boolean doEvaluate(C21Paths t) {
			Boolean ok = null;
			return assignOutput(ok, t);
		}
		
		protected Boolean assignOutput(Boolean ok, C21Paths t) {
			if (onlyExists(MapperS.of(t), Arrays.asList("p", "q", "r"), Arrays.asList("p")).getOrDefault(false)) {
				ok = true;
			} else {
				ok = onlyExists(MapperS.of(t), Arrays.asList("p", "q", "r"), Arrays.asList("q", "r")).get();
			}
			
			return ok;
		}
	}
}
