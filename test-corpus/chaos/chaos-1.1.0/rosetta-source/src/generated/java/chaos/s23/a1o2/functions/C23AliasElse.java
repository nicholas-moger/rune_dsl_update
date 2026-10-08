package chaos.s23.a1o2.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import java.math.BigInteger;


@ImplementedBy(C23AliasElse.C23AliasElseDefault.class)
public abstract class C23AliasElse implements RosettaFunction {

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

	protected abstract MapperS<BigInteger> huge(BigDecimal v, Boolean f);

	public static class C23AliasElseDefault extends C23AliasElse {
		@Override
		protected BigDecimal doEvaluate(BigDecimal v, Boolean f) {
			BigDecimal r = null;
			return assignOutput(r, v, f);
		}
		
		protected BigDecimal assignOutput(BigDecimal r, BigDecimal v, Boolean f) {
			if ((f == null ? false : f)) {
				r = v;
			} else {
				final BigInteger bigInteger = huge(v, f).get();
				if (bigInteger == null) {
					r = null;
				} else {
					r = new BigDecimal(bigInteger);
				}
			}
			
			return r;
		}
		
		@Override
		protected MapperS<BigInteger> huge(BigDecimal v, Boolean f) {
			return MapperS.of(new BigInteger("9999999999999999999999999"));
		}
	}
}
