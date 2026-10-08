package chaos.s08.base.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.MapperMaths;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;


@ImplementedBy(C8Scale.C8ScaleDefault.class)
public abstract class C8Scale implements RosettaFunction {

	/**
	* @param q 
	* @param w 
	* @return r 
	*/
	public BigDecimal evaluate(Integer q, BigDecimal w) {
		BigDecimal r = doEvaluate(q, w);
		
		return r;
	}

	protected abstract BigDecimal doEvaluate(Integer q, BigDecimal w);

	public static class C8ScaleDefault extends C8Scale {
		@Override
		protected BigDecimal doEvaluate(Integer q, BigDecimal w) {
			BigDecimal r = null;
			return assignOutput(r, q, w);
		}
		
		protected BigDecimal assignOutput(BigDecimal r, Integer q, BigDecimal w) {
			r = MapperMaths.<BigDecimal, BigDecimal, BigDecimal>divide(MapperMaths.<BigDecimal, BigDecimal, BigDecimal>multiply((q == null ? MapperS.<BigDecimal>ofNull() : MapperS.of(BigDecimal.valueOf(q))), MapperS.of(w)), MapperS.of(BigDecimal.valueOf(100))).get();
			
			return r;
		}
	}
}
