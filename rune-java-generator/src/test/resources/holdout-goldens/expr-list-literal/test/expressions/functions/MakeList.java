package test.expressions.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


@ImplementedBy(MakeList.MakeListDefault.class)
public abstract class MakeList implements RosettaFunction {

	/**
	* @param x 
	* @return result 
	*/
	public List<BigDecimal> evaluate(BigDecimal x) {
		List<BigDecimal> result = doEvaluate(x);
		
		return result;
	}

	protected abstract List<BigDecimal> doEvaluate(BigDecimal x);

	public static class MakeListDefault extends MakeList {
		@Override
		protected List<BigDecimal> doEvaluate(BigDecimal x) {
			List<BigDecimal> result = new ArrayList<>();
			return assignOutput(result, x);
		}
		
		protected List<BigDecimal> assignOutput(List<BigDecimal> result, BigDecimal x) {
			result = MapperC.<Integer>of(MapperS.of(1), MapperS.of(2), MapperS.of(3)).<BigDecimal>map("Type coercion", integer -> BigDecimal.valueOf(integer)).getMulti();
			
			return result;
		}
	}
}
