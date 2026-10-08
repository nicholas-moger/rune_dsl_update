package chaos.s23.a5uni.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import java.math.BigDecimal;
import java.math.BigInteger;
import javax.inject.Inject;


@ImplementedBy(C23IntOut.C23IntOutDefault.class)
public abstract class C23IntOut implements RosettaFunction {
	
	// RosettaFunction dependencies
	//
	@Inject protected C23IsBig c23IsBig;

	/**
	* @param v 
	* @return r 
	*/
	public Integer evaluate(BigDecimal v) {
		Integer r = doEvaluate(v);
		
		return r;
	}

	protected abstract Integer doEvaluate(BigDecimal v);

	public static class C23IntOutDefault extends C23IntOut {
		@Override
		protected Integer doEvaluate(BigDecimal v) {
			Integer r = null;
			return assignOutput(r, v);
		}
		
		protected Integer assignOutput(Integer r, BigDecimal v) {
			final Boolean _boolean = c23IsBig.evaluate(v);
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
