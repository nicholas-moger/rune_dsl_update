package test.expressions.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.MapperMaths;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;


@ImplementedBy(ReduceItems.ReduceItemsDefault.class)
public abstract class ReduceItems implements RosettaFunction {

	/**
	* @param items 
	* @return result 
	*/
	public BigDecimal evaluate(List<BigDecimal> items) {
		BigDecimal result = doEvaluate(items);
		
		return result;
	}

	protected abstract BigDecimal doEvaluate(List<BigDecimal> items);

	public static class ReduceItemsDefault extends ReduceItems {
		@Override
		protected BigDecimal doEvaluate(List<BigDecimal> items) {
			if (items == null) {
				items = Collections.emptyList();
			}
			BigDecimal result = null;
			return assignOutput(result, items);
		}
		
		protected BigDecimal assignOutput(BigDecimal result, List<BigDecimal> items) {
			result = MapperC.<BigDecimal>of(items)
				.<BigDecimal>reduce((a, b) -> MapperMaths.<BigDecimal, BigDecimal, BigDecimal>add(a, b)).get();
			
			return result;
		}
	}
}
