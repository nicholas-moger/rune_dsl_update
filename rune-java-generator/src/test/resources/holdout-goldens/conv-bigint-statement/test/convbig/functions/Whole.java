package test.convbig.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import java.math.BigDecimal;
import java.math.BigInteger;


@ImplementedBy(Whole.WholeDefault.class)
public abstract class Whole implements RosettaFunction {

	/**
	* @param v 
	* @return r 
	*/
	public BigDecimal evaluate(BigDecimal v) {
		BigDecimal r = doEvaluate(v);
		
		return r;
	}

	protected abstract BigDecimal doEvaluate(BigDecimal v);

	public static class WholeDefault extends Whole {
		@Override
		protected BigDecimal doEvaluate(BigDecimal v) {
			BigDecimal r = null;
			return assignOutput(r, v);
		}
		
		protected BigDecimal assignOutput(BigDecimal r, BigDecimal v) {
			final BigInteger bigInteger = new BigInteger("9999999999999999999999999");
			if (bigInteger == null) {
				r = null;
			} else {
				r = new BigDecimal(bigInteger);
			}
			
			return r;
		}
	}
}
