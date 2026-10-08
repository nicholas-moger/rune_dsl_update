package test.convbig.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;


@ImplementedBy(MultiThen.MultiThenDefault.class)
public abstract class MultiThen implements RosettaFunction {
	
	// RosettaFunction dependencies
	//
	@Inject protected IsBig isBig;

	/**
	* @param v 
	* @return rs 
	*/
	public List<BigDecimal> evaluate(BigDecimal v) {
		List<BigDecimal> rs = doEvaluate(v);
		
		return rs;
	}

	protected abstract List<BigDecimal> doEvaluate(BigDecimal v);

	public static class MultiThenDefault extends MultiThen {
		@Override
		protected List<BigDecimal> doEvaluate(BigDecimal v) {
			List<BigDecimal> rs = new ArrayList<>();
			return assignOutput(rs, v);
		}
		
		protected List<BigDecimal> assignOutput(List<BigDecimal> rs, BigDecimal v) {
			final Boolean _boolean = isBig.evaluate(v);
			if ((_boolean == null ? false : _boolean)) {
				final BigInteger bigInteger = new BigInteger("9999999999999999999999999");
				if (bigInteger == null) {
					rs = Collections.<BigDecimal>emptyList();
				} else {
					rs = Collections.singletonList(new BigDecimal(bigInteger));
				}
			} else if (v == null) {
				rs = Collections.<BigDecimal>emptyList();
			} else {
				rs = Collections.singletonList(v);
			}
			
			return rs;
		}
	}
}
