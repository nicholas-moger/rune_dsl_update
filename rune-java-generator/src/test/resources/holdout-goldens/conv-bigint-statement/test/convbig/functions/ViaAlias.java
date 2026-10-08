package test.convbig.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import java.math.BigInteger;
import javax.inject.Inject;


@ImplementedBy(ViaAlias.ViaAliasDefault.class)
public abstract class ViaAlias implements RosettaFunction {
	
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

	protected abstract MapperS<BigInteger> big(BigDecimal v);

	public static class ViaAliasDefault extends ViaAlias {
		@Override
		protected BigDecimal doEvaluate(BigDecimal v) {
			BigDecimal r = null;
			return assignOutput(r, v);
		}
		
		protected BigDecimal assignOutput(BigDecimal r, BigDecimal v) {
			final Boolean _boolean = isBig.evaluate(v);
			if ((_boolean == null ? false : _boolean)) {
				final BigInteger bigInteger = big(v).get();
				if (bigInteger == null) {
					r = null;
				} else {
					r = new BigDecimal(bigInteger);
				}
			} else {
				r = v;
			}
			
			return r;
		}
		
		@Override
		protected MapperS<BigInteger> big(BigDecimal v) {
			return MapperS.of(new BigInteger("9999999999999999999999999"));
		}
	}
}
