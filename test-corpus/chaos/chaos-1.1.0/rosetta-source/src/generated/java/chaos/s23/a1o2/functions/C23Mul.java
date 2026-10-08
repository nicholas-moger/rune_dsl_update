package chaos.s23.a1o2.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.MapperMaths;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;


@ImplementedBy(C23Mul.C23MulDefault.class)
public abstract class C23Mul implements RosettaFunction {

	/**
	* @param a 
	* @param b 
	* @return p 
	*/
	public BigDecimal evaluate(BigDecimal a, BigDecimal b) {
		BigDecimal p = doEvaluate(a, b);
		
		return p;
	}

	protected abstract BigDecimal doEvaluate(BigDecimal a, BigDecimal b);

	public static class C23MulDefault extends C23Mul {
		@Override
		protected BigDecimal doEvaluate(BigDecimal a, BigDecimal b) {
			BigDecimal p = null;
			return assignOutput(p, a, b);
		}
		
		protected BigDecimal assignOutput(BigDecimal p, BigDecimal a, BigDecimal b) {
			p = MapperMaths.<BigDecimal, BigDecimal, BigDecimal>multiply(MapperS.of(a), MapperS.of(b)).get();
			
			return p;
		}
	}
}
