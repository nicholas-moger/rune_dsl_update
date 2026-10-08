package holdout.closureparamduplicate.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.MapperMaths;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import holdout.closureparamduplicate.Item;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;


@ImplementedBy(ReduceDup.ReduceDupDefault.class)
public abstract class ReduceDup implements RosettaFunction {

	/**
	* @param items 
	* @return total 
	*/
	public BigDecimal evaluate(List<? extends Item> items) {
		BigDecimal total = doEvaluate(items);
		
		return total;
	}

	protected abstract BigDecimal doEvaluate(List<? extends Item> items);

	public static class ReduceDupDefault extends ReduceDup {
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
				.<BigDecimal>reduce((a0, a1) -> MapperMaths.<BigDecimal, BigDecimal, BigDecimal>add(a0, a0)).get();
			
			return total;
		}
	}
}
