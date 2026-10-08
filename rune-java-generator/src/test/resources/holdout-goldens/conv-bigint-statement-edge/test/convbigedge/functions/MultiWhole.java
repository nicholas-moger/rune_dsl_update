package test.convbigedge.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@ImplementedBy(MultiWhole.MultiWholeDefault.class)
public abstract class MultiWhole implements RosettaFunction {

	/**
	* @param v 
	* @return rs 
	*/
	public List<BigDecimal> evaluate(BigDecimal v) {
		List<BigDecimal> rs = doEvaluate(v);
		
		return rs;
	}

	protected abstract List<BigDecimal> doEvaluate(BigDecimal v);

	public static class MultiWholeDefault extends MultiWhole {
		@Override
		protected List<BigDecimal> doEvaluate(BigDecimal v) {
			List<BigDecimal> rs = new ArrayList<>();
			return assignOutput(rs, v);
		}
		
		protected List<BigDecimal> assignOutput(List<BigDecimal> rs, BigDecimal v) {
			final BigInteger bigInteger = new BigInteger("9999999999999999999999999");
			if (bigInteger == null) {
				rs = Collections.<BigDecimal>emptyList();
			} else {
				rs = Collections.singletonList(new BigDecimal(bigInteger));
			}
			
			return rs;
		}
	}
}
