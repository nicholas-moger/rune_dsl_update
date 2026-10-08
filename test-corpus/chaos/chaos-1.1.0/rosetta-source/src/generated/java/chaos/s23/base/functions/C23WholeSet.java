package chaos.s23.base.functions;

import chaos.s23.base.C23Box;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@ImplementedBy(C23WholeSet.C23WholeSetDefault.class)
public abstract class C23WholeSet implements RosettaFunction {

	/**
	* @param b 
	* @return rs 
	*/
	public List<BigDecimal> evaluate(C23Box b) {
		List<BigDecimal> rs = doEvaluate(b);
		
		return rs;
	}

	protected abstract List<BigDecimal> doEvaluate(C23Box b);

	public static class C23WholeSetDefault extends C23WholeSet {
		@Override
		protected List<BigDecimal> doEvaluate(C23Box b) {
			List<BigDecimal> rs = new ArrayList<>();
			return assignOutput(rs, b);
		}
		
		protected List<BigDecimal> assignOutput(List<BigDecimal> rs, C23Box b) {
			final BigInteger bigInteger = new BigInteger("9999999999999999999999999");
			if (bigInteger == null) {
				rs.addAll(Collections.<BigDecimal>emptyList());
			} else {
				rs.addAll(Collections.singletonList(new BigDecimal(bigInteger)));
			}
			
			final BigDecimal bigDecimal = MapperS.of(b).<BigDecimal>map("getWeight", c23Box -> c23Box.getWeight()).getOrDefault(BigDecimal.valueOf(0));
			if (bigDecimal == null) {
				rs.addAll(Collections.<BigDecimal>emptyList());
			} else {
				rs.addAll(Collections.singletonList(bigDecimal));
			}
			
			rs.addAll(MapperC.<Integer>of(MapperS.of(1), MapperS.of(2), MapperS.of(3)).<BigDecimal>map("Type coercion", integer -> BigDecimal.valueOf(integer)).getMulti());
			
			return rs;
		}
	}
}
