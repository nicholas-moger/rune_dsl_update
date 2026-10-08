package test.dispatchns.c.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.MapperMaths;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;


@ImplementedBy(Speed.SpeedDefault.class)
public abstract class Speed implements RosettaFunction {

	/**
	* @param x 
	* @return y 
	*/
	public BigDecimal evaluate(BigDecimal x) {
		BigDecimal y = doEvaluate(x);
		
		return y;
	}

	protected abstract BigDecimal doEvaluate(BigDecimal x);

	public static class SpeedDefault extends Speed {
		@Override
		protected BigDecimal doEvaluate(BigDecimal x) {
			BigDecimal y = null;
			return assignOutput(y, x);
		}
		
		protected BigDecimal assignOutput(BigDecimal y, BigDecimal x) {
			y = MapperMaths.<BigDecimal, BigDecimal, BigDecimal>multiply(MapperS.of(x), MapperS.of(BigDecimal.valueOf(4))).get();
			
			return y;
		}
	}
}
