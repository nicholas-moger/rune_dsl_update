package test.convbigedge.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import java.math.BigDecimal;
import java.math.BigInteger;
import javax.inject.Inject;


@ImplementedBy(BothArms.BothArmsDefault.class)
public abstract class BothArms implements RosettaFunction {
	
	// RosettaFunction dependencies
	//
	@Inject protected IsBig isBig;

	/**
	* @param v 
	* @return r 
	*/
	public BigDecimal evaluate(BigDecimal v) {
		BigDecimal r = doEvaluate(v);
		
		return r;
	}

	protected abstract BigDecimal doEvaluate(BigDecimal v);

	public static class BothArmsDefault extends BothArms {
		@Override
		protected BigDecimal doEvaluate(BigDecimal v) {
			BigDecimal r = null;
			return assignOutput(r, v);
		}
		
		protected BigDecimal assignOutput(BigDecimal r, BigDecimal v) {
			final Boolean _boolean = isBig.evaluate(v);
			if ((_boolean == null ? false : _boolean)) {
				final BigInteger bigInteger0 = new BigInteger("9999999999999999999999999");
				if (bigInteger0 == null) {
					r = null;
				} else {
					r = new BigDecimal(bigInteger0);
				}
			} else {
				final BigInteger bigInteger1 = new BigInteger("8888888888888888888888888");
				if (bigInteger1 == null) {
					r = null;
				} else {
					r = new BigDecimal(bigInteger1);
				}
			}
			
			return r;
		}
	}
}
