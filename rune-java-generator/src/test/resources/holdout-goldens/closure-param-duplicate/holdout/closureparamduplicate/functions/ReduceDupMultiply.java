package holdout.closureparamduplicate.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.MapperMaths;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import holdout.closureparamduplicate.Item;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;


@ImplementedBy(ReduceDupMultiply.ReduceDupMultiplyDefault.class)
public abstract class ReduceDupMultiply implements RosettaFunction {

	/**
	* @param items 
	* @return product 
	*/
	public BigDecimal evaluate(List<? extends Item> items) {
		BigDecimal product = doEvaluate(items);
		
		return product;
	}

	protected abstract BigDecimal doEvaluate(List<? extends Item> items);

	public static class ReduceDupMultiplyDefault extends ReduceDupMultiply {
		@Override
		protected BigDecimal doEvaluate(List<? extends Item> items) {
			if (items == null) {
				items = Collections.emptyList();
			}
			BigDecimal product = null;
			return assignOutput(product, items);
		}
		
		protected BigDecimal assignOutput(BigDecimal product, List<? extends Item> items) {
			final MapperC<BigDecimal> thenArg = MapperC.<Item>of(items)
				.mapItem(i -> i.<BigDecimal>map("getV", item -> item.getV()));
			product = thenArg
				.<BigDecimal>reduce((a0, a1) -> MapperMaths.<BigDecimal, BigDecimal, BigDecimal>multiply(a0, a0)).get();
			
			return product;
		}
	}
}
