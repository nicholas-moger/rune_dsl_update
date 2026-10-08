package holdout.closureparamduplicate.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.expression.MapperMaths;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import holdout.closureparamduplicate.Item;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(ReduceDupThenUse.ReduceDupThenUseDefault.class)
public abstract class ReduceDupThenUse implements RosettaFunction {

	/**
	* @param items 
	* @return total 
	*/
	public BigDecimal evaluate(List<? extends Item> items) {
		BigDecimal total = doEvaluate(items);
		
		return total;
	}

	protected abstract BigDecimal doEvaluate(List<? extends Item> items);

	public static class ReduceDupThenUseDefault extends ReduceDupThenUse {
		@Override
		protected BigDecimal doEvaluate(List<? extends Item> items) {
			if (items == null) {
				items = Collections.emptyList();
			}
			BigDecimal total = null;
			return assignOutput(total, items);
		}
		
		protected BigDecimal assignOutput(BigDecimal total, List<? extends Item> items) {
			final MapperC<BigDecimal> thenArg = MapperC.<Item>of(items)
				.mapItem(i -> i.<BigDecimal>map("getV", item -> item.getV()));
			total = thenArg
				.<BigDecimal>reduce((a0, a1) -> {
					if (greaterThan(a0, MapperS.of(BigDecimal.valueOf(0)), CardinalityOperator.All).getOrDefault(false)) {
						return MapperMaths.<BigDecimal, BigDecimal, BigDecimal>add(a0, a0);
					}
					return a0;
				}).get();
			
			return total;
		}
	}
}
