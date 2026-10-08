package test.convbigedge.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import java.math.BigDecimal;
import java.math.BigInteger;
import javax.inject.Inject;


@ImplementedBy(IntOut.IntOutDefault.class)
public abstract class IntOut implements RosettaFunction {
	
	// RosettaFunction dependencies
	//
	@Inject protected IsBig isBig;

	/**
	* @param v 
	* @return r 
	*/
	public Integer evaluate(BigDecimal v) {
		Integer r = doEvaluate(v);
		
		return r;
	}

	protected abstract Integer doEvaluate(BigDecimal v);

	public static class IntOutDefault extends IntOut {
		@Override
		protected Integer doEvaluate(BigDecimal v) {
			Integer r = null;
			return assignOutput(r, v);
		}
		
		protected Integer assignOutput(Integer r, BigDecimal v) {
			final Boolean _boolean = isBig.evaluate(v);
			if ((_boolean == null ? false : _boolean)) {
				final BigInteger bigInteger = new BigInteger("9999999999999999999999999");
				if (bigInteger == null) {
					r = null;
				} else {
					r = bigInteger.intValueExact();
				}
			} else {
				r = 1;
			}
			
			return r;
		}
	}
}
