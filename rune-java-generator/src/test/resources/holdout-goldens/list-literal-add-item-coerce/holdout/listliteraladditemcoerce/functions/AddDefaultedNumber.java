package holdout.listliteraladditemcoerce.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import holdout.listliteraladditemcoerce.Box;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@ImplementedBy(AddDefaultedNumber.AddDefaultedNumberDefault.class)
public abstract class AddDefaultedNumber implements RosettaFunction {

	/**
	* @param b 
	* @return rs 
	*/
	public List<BigDecimal> evaluate(Box b) {
		List<BigDecimal> rs = doEvaluate(b);
		
		return rs;
	}

	protected abstract List<BigDecimal> doEvaluate(Box b);

	public static class AddDefaultedNumberDefault extends AddDefaultedNumber {
		@Override
		protected List<BigDecimal> doEvaluate(Box b) {
			List<BigDecimal> rs = new ArrayList<>();
			return assignOutput(rs, b);
		}
		
		protected List<BigDecimal> assignOutput(List<BigDecimal> rs, Box b) {
			final BigDecimal bigDecimal = MapperS.of(b).<BigDecimal>map("getWeight", box -> box.getWeight()).getOrDefault(BigDecimal.valueOf(0));
			if (bigDecimal == null) {
				rs.addAll(Collections.<BigDecimal>emptyList());
			} else {
				rs.addAll(Collections.singletonList(bigDecimal));
			}
			
			return rs;
		}
	}
}
