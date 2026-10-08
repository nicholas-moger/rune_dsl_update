package test.convbigedge.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import java.math.BigDecimal;
import java.math.BigInteger;
import javax.inject.Inject;


@ImplementedBy(Nested.NestedDefault.class)
public abstract class Nested implements RosettaFunction {
	
	// RosettaFunction dependencies
	//
	@Inject protected IsBig isBig;

	/**
	* @param v 
	* @param f 
	* @return r 
	*/
	public BigDecimal evaluate(BigDecimal v, Boolean f) {
		BigDecimal r = doEvaluate(v, f);
		
		return r;
	}

	protected abstract BigDecimal doEvaluate(BigDecimal v, Boolean f);

	public static class NestedDefault extends Nested {
		@Override
		protected BigDecimal doEvaluate(BigDecimal v, Boolean f) {
			BigDecimal r = null;
			return assignOutput(r, v, f);
		}
		
		protected BigDecimal assignOutput(BigDecimal r, BigDecimal v, Boolean f) {
			if ((f == null ? false : f)) {
				final Boolean _boolean = isBig.evaluate(v);
				if ((_boolean == null ? false : _boolean)) {
					final BigInteger bigInteger = new BigInteger("9999999999999999999999999");
					if (bigInteger == null) {
						r = null;
					} else {
						r = new BigDecimal(bigInteger);
					}
				} else {
					r = v;
				}
			} else {
				r = BigDecimal.valueOf(0);
			}
			
			return r;
		}
	}
}
