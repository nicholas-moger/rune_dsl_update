package chaos.s23.a2wild.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C23CmpArm.C23CmpArmDefault.class)
public abstract class C23CmpArm implements RosettaFunction {

	/**
	* @param flag 
	* @param n 
	* @return ok 
	*/
	public Boolean evaluate(Boolean flag, BigDecimal n) {
		Boolean ok = doEvaluate(flag, n);
		
		return ok;
	}

	protected abstract Boolean doEvaluate(Boolean flag, BigDecimal n);

	public static class C23CmpArmDefault extends C23CmpArm {
		@Override
		protected Boolean doEvaluate(Boolean flag, BigDecimal n) {
			Boolean ok = null;
			return assignOutput(ok, flag, n);
		}
		
		protected Boolean assignOutput(Boolean ok, Boolean flag, BigDecimal n) {
			final MapperS<String> ifThenElseResult0;
			if ((flag == null ? false : flag)) {
				ifThenElseResult0 = MapperS.of("a");
			} else {
				ifThenElseResult0 = MapperS.of("b");
			}
			final MapperS<BigDecimal> ifThenElseResult1;
			if ((flag == null ? false : flag)) {
				ifThenElseResult1 = MapperS.of(BigDecimal.valueOf(1));
			} else {
				ifThenElseResult1 = MapperS.of(BigDecimal.valueOf(2));
			}
			final MapperS<BigDecimal> ifThenElseResult2;
			if ((flag == null ? false : flag)) {
				ifThenElseResult2 = MapperS.of(BigDecimal.valueOf(0));
			} else {
				ifThenElseResult2 = MapperS.of(BigDecimal.valueOf(1));
			}
			ok = areEqual(ifThenElseResult0, MapperS.of("a"), CardinalityOperator.All).andNullSafe(lessThan(ifThenElseResult1, MapperS.of(n), CardinalityOperator.All)).andNullSafe(greaterThanEquals(MapperS.of(n), ifThenElseResult2, CardinalityOperator.All)).get();
			
			return ok;
		}
	}
}
