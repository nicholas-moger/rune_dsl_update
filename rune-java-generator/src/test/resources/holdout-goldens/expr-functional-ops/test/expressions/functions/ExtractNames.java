package test.expressions.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.MapperMaths;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@ImplementedBy(ExtractNames.ExtractNamesDefault.class)
public abstract class ExtractNames implements RosettaFunction {

	/**
	* @param items 
	* @return result 
	*/
	public List<BigDecimal> evaluate(List<BigDecimal> items) {
		List<BigDecimal> result = doEvaluate(items);
		
		return result;
	}

	protected abstract List<BigDecimal> doEvaluate(List<BigDecimal> items);

	public static class ExtractNamesDefault extends ExtractNames {
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
				.mapItem(item -> MapperMaths.<BigDecimal, BigDecimal, BigDecimal>add(item, MapperS.of(BigDecimal.valueOf(1)))).getMulti();
			
			return result;
		}
	}
}
