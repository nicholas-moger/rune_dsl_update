package test.expressions.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(FilterItems.FilterItemsDefault.class)
public abstract class FilterItems implements RosettaFunction {

	/**
	* @param items 
	* @return result 
	*/
	public List<BigDecimal> evaluate(List<BigDecimal> items) {
		List<BigDecimal> result = doEvaluate(items);
		
		return result;
	}

	protected abstract List<BigDecimal> doEvaluate(List<BigDecimal> items);

	public static class FilterItemsDefault extends FilterItems {
		@Override
		protected List<BigDecimal> doEvaluate(List<BigDecimal> items) {
			if (items == null) {
				items = Collections.emptyList();
			}
			List<BigDecimal> result = new ArrayList<>();
			return assignOutput(result, items);
		}
		
		protected List<BigDecimal> assignOutput(List<BigDecimal> result, List<BigDecimal> items) {
			result = MapperC.<BigDecimal>of(items)
				.filterItemNullSafe(item -> greaterThan(item, MapperS.of(BigDecimal.valueOf(0)), CardinalityOperator.All).get()).getMulti();
			
			return result;
		}
	}
}
