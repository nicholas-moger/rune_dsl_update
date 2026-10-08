package test.convbigedge.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import java.math.BigDecimal;
import java.math.BigInteger;


@ImplementedBy(IdentityOut.IdentityOutDefault.class)
public abstract class IdentityOut implements RosettaFunction {

	/**
	* @param v 
	* @return r 
	*/
	public BigInteger evaluate(BigDecimal v) {
		BigInteger r = doEvaluate(v);
		
		return r;
	}

	protected abstract BigInteger doEvaluate(BigDecimal v);

	public static class IdentityOutDefault extends IdentityOut {
		@Override
		protected BigInteger doEvaluate(BigDecimal v) {
			BigInteger r = null;
			return assignOutput(r, v);
		}
		
		protected BigInteger assignOutput(BigInteger r, BigDecimal v) {
			r = new BigInteger("9999999999999999999999999");
			
			return r;
		}
	}
}
