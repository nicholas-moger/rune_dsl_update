package chaos.s04.a5crlf.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.MapperMaths;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;


@ImplementedBy(C4Add.C4AddDefault.class)
public abstract class C4Add implements RosettaFunction {

	/**
	* @param a 
	* @param b 
	* @return s 
	*/
	public BigDecimal evaluate(BigDecimal a, BigDecimal b) {
		BigDecimal s = doEvaluate(a, b);
		
		return s;
	}

	protected abstract BigDecimal doEvaluate(BigDecimal a, BigDecimal b);

	public static class C4AddDefault extends C4Add {
		@Override
		protected BigDecimal doEvaluate(BigDecimal a, BigDecimal b) {
			BigDecimal s = null;
			return assignOutput(s, a, b);
		}
		
		protected BigDecimal assignOutput(BigDecimal s, BigDecimal a, BigDecimal b) {
			s = MapperMaths.<BigDecimal, BigDecimal, BigDecimal>add(MapperS.of(a), MapperS.of(b)).get();
			
			return s;
		}
	}
}
