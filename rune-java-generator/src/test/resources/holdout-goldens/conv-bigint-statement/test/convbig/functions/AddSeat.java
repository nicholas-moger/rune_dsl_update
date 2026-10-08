package test.convbig.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@ImplementedBy(AddSeat.AddSeatDefault.class)
public abstract class AddSeat implements RosettaFunction {

	/**
	* @param v 
	* @return rs 
	*/
	public List<BigDecimal> evaluate(BigDecimal v) {
		List<BigDecimal> rs = doEvaluate(v);
		
		return rs;
	}

	protected abstract List<BigDecimal> doEvaluate(BigDecimal v);

	public static class AddSeatDefault extends AddSeat {
		@Override
		protected List<BigDecimal> doEvaluate(BigDecimal v) {
			List<BigDecimal> rs = new ArrayList<>();
			return assignOutput(rs, v);
		}
		
		protected List<BigDecimal> assignOutput(List<BigDecimal> rs, BigDecimal v) {
			if (v == null) {
				rs.addAll(Collections.<BigDecimal>emptyList());
			} else {
				rs.addAll(Collections.singletonList(v));
			}
			
			final BigInteger bigInteger = new BigInteger("9999999999999999999999999");
			if (bigInteger == null) {
				rs.addAll(Collections.<BigDecimal>emptyList());
			} else {
				rs.addAll(Collections.singletonList(new BigDecimal(bigInteger)));
			}
			
			return rs;
		}
	}
}
