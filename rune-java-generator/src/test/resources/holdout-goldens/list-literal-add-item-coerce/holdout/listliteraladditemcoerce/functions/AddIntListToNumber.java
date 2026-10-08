package holdout.listliteraladditemcoerce.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


@ImplementedBy(AddIntListToNumber.AddIntListToNumberDefault.class)
public abstract class AddIntListToNumber implements RosettaFunction {

	/**
	* @return rs 
	*/
	public List<BigDecimal> evaluate() {
		List<BigDecimal> rs = doEvaluate();
		
		return rs;
	}

	protected abstract List<BigDecimal> doEvaluate();

	public static class AddIntListToNumberDefault extends AddIntListToNumber {
		@Override
		protected List<BigDecimal> doEvaluate() {
			List<BigDecimal> rs = new ArrayList<>();
			return assignOutput(rs);
		}
		
		protected List<BigDecimal> assignOutput(List<BigDecimal> rs) {
			rs.addAll(MapperC.<Integer>of(MapperS.of(1), MapperS.of(2), MapperS.of(3)).<BigDecimal>map("Type coercion", integer -> BigDecimal.valueOf(integer)).getMulti());
			
			return rs;
		}
	}
}
