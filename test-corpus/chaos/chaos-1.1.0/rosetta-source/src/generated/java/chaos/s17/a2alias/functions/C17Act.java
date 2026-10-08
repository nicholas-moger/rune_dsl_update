package chaos.s17.a2alias.functions;

import chaos.s17.a2alias.C17ActionEnum;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C17Act.C17ActDefault.class)
public abstract class C17Act implements RosettaFunction {

	/**
	* @param a 
	* @return ok 
	*/
	public Boolean evaluate(C17ActionEnum a) {
		Boolean ok = doEvaluate(a);
		
		return ok;
	}

	protected abstract Boolean doEvaluate(C17ActionEnum a);

	public static class C17ActDefault extends C17Act {
		@Override
		protected Boolean doEvaluate(C17ActionEnum a) {
			Boolean ok = null;
			return assignOutput(ok, a);
		}
		
		protected Boolean assignOutput(Boolean ok, C17ActionEnum a) {
			ok = areEqual(MapperS.of(a), MapperS.of(C17ActionEnum.BUY), CardinalityOperator.All).orNullSafe(areEqual(MapperS.of(a), MapperS.of(C17ActionEnum.AMEND), CardinalityOperator.All)).get();
			
			return ok;
		}
	}
}
