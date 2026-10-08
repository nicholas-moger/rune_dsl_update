package test.convbig.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.MapperMaths;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import java.math.BigInteger;
import javax.inject.Inject;


@ImplementedBy(ThenArm.ThenArmDefault.class)
public abstract class ThenArm implements RosettaFunction {
	
	// RosettaFunction dependencies
	//
	@Inject protected IsBig isBig;

	/**
	* @param v 
	* @param w 
	* @return r 
	*/
	public BigDecimal evaluate(BigDecimal v, BigDecimal w) {
		BigDecimal r = doEvaluate(v, w);
		
		return r;
	}

	protected abstract BigDecimal doEvaluate(BigDecimal v, BigDecimal w);

	public static class ThenArmDefault extends ThenArm {
		@Override
		protected BigDecimal doEvaluate(BigDecimal v, BigDecimal w) {
			BigDecimal r = null;
			return assignOutput(r, v, w);
		}
		
		protected BigDecimal assignOutput(BigDecimal r, BigDecimal v, BigDecimal w) {
			final Boolean _boolean = isBig.evaluate(v);
			if ((_boolean == null ? false : _boolean)) {
				final BigInteger bigInteger = new BigInteger("9999999999999999999999999");
				if (bigInteger == null) {
					r = null;
				} else {
					r = new BigDecimal(bigInteger);
				}
			} else {
				r = MapperMaths.<BigDecimal, BigDecimal, BigDecimal>add(MapperS.of(v), MapperS.of(MapperS.of(w).getOrDefault(BigDecimal.valueOf(0)))).get();
			}
			
			return r;
		}
	}
}
