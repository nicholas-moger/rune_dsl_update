package chaos.s18.a1o3.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C18Grade.C18GradeDefault.class)
public abstract class C18Grade implements RosettaFunction {

	/**
	* @param g 
	* @return n 
	*/
	public BigDecimal evaluate(String g) {
		BigDecimal n = doEvaluate(g);
		
		return n;
	}

	protected abstract BigDecimal doEvaluate(String g);

	public static class C18GradeDefault extends C18Grade {
		@Override
		protected BigDecimal doEvaluate(String g) {
			BigDecimal n = null;
			return assignOutput(n, g);
		}
		
		protected BigDecimal assignOutput(BigDecimal n, String g) {
			final MapperS<String> switchArgument = MapperS.of(g);
			if (switchArgument.get() == null) {
				n = null;
			} else if (areEqual(switchArgument, MapperS.of("A"), CardinalityOperator.All).get()) {
				n = BigDecimal.valueOf(1);
			} else if (areEqual(switchArgument, MapperS.of("B"), CardinalityOperator.All).get()) {
				n = BigDecimal.valueOf(2);
			} else {
				n = BigDecimal.valueOf(0);
			}
			
			return n;
		}
	}
}
