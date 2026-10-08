package chaos.s04.a2qual.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C4IsBig.C4IsBigDefault.class)
public abstract class C4IsBig implements RosettaFunction {

	/**
	* @param v 
	* @return big 
	*/
	public Boolean evaluate(BigDecimal v) {
		Boolean big = doEvaluate(v);
		
		return big;
	}

	protected abstract Boolean doEvaluate(BigDecimal v);

	public static class C4IsBigDefault extends C4IsBig {
		@Override
		protected Boolean doEvaluate(BigDecimal v) {
			Boolean big = null;
			return assignOutput(big, v);
		}
		
		protected Boolean assignOutput(Boolean big, BigDecimal v) {
			big = greaterThan(MapperS.of(v), MapperS.of(BigDecimal.valueOf(1000000)), CardinalityOperator.All).get();
			
			return big;
		}
	}
}
