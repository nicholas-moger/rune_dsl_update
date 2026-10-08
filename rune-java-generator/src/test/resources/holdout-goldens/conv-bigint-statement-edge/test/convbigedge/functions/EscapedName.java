package test.convbigedge.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import java.math.BigDecimal;
import java.math.BigInteger;
import javax.inject.Inject;


@ImplementedBy(EscapedName.EscapedNameDefault.class)
public abstract class EscapedName implements RosettaFunction {
	
	// RosettaFunction dependencies
	//
	@Inject protected IsBig isBig;

	/**
	* @param v 
	* @return _transient 
	*/
	public BigDecimal evaluate(BigDecimal v) {
		BigDecimal _transient = doEvaluate(v);
		
		return _transient;
	}

	protected abstract BigDecimal doEvaluate(BigDecimal v);

	public static class EscapedNameDefault extends EscapedName {
		@Override
		protected BigDecimal doEvaluate(BigDecimal v) {
			BigDecimal _transient = null;
			return assignOutput(_transient, v);
		}
		
		protected BigDecimal assignOutput(BigDecimal _transient, BigDecimal v) {
			final Boolean _boolean = isBig.evaluate(v);
			if ((_boolean == null ? false : _boolean)) {
				final BigInteger bigInteger = new BigInteger("9999999999999999999999999");
				if (bigInteger == null) {
					_transient = null;
				} else {
					_transient = new BigDecimal(bigInteger);
				}
			} else {
				_transient = v;
			}
			
			return _transient;
		}
	}
}
