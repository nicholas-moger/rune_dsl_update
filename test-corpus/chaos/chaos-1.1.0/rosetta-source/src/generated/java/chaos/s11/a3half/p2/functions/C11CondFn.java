package chaos.s11.a3half.p2.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C11CondFn.C11CondFnDefault.class)
public abstract class C11CondFn implements RosettaFunction {

	/**
	* @param raw 
	* @return ok 
	*/
	public Boolean evaluate(String raw) {
		Boolean ok = doEvaluate(raw);
		
		return ok;
	}

	protected abstract Boolean doEvaluate(String raw);

	public static class C11CondFnDefault extends C11CondFn {
		@Override
		protected Boolean doEvaluate(String raw) {
			Boolean ok = null;
			return assignOutput(ok, raw);
		}
		
		protected Boolean assignOutput(Boolean ok, String raw) {
			ok = exists(MapperS.of(raw)).get();
			
			return ok;
		}
	}
}
