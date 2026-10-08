package test.convbigedge.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import java.math.BigDecimal;
import java.math.BigInteger;
import javax.inject.Inject;


@ImplementedBy(AsArg.AsArgDefault.class)
public abstract class AsArg implements RosettaFunction {
	
	// RosettaFunction dependencies
	//
	@Inject protected IsBig isBig;

	/**
	* @param v 
	* @return r 
	*/
	public Boolean evaluate(BigDecimal v) {
		Boolean r = doEvaluate(v);
		
		return r;
	}

	protected abstract Boolean doEvaluate(BigDecimal v);

	public static class AsArgDefault extends AsArg {
		@Override
		protected Boolean doEvaluate(BigDecimal v) {
			Boolean r = null;
			return assignOutput(r, v);
		}
		
		protected Boolean assignOutput(Boolean r, BigDecimal v) {
			final BigInteger bigInteger = new BigInteger("9999999999999999999999999");
			r = isBig.evaluate((bigInteger == null ? null : new BigDecimal(bigInteger)));
			
			return r;
		}
	}
}
