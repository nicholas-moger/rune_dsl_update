package chaos.s95.base.functions;

import chaos.s95.base.C95Item;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.MapperMaths;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;


@ImplementedBy(C95Reduce.C95ReduceDefault.class)
public abstract class C95Reduce implements RosettaFunction {

	/**
	* @param items 
	* @return total 
	*/
	public BigDecimal evaluate(List<? extends C95Item> items) {
		BigDecimal total = doEvaluate(items);
		
		return total;
	}

	protected abstract BigDecimal doEvaluate(List<? extends C95Item> items);

	public static class C95ReduceDefault extends C95Reduce {
		@Override
		protected BigDecimal doEvaluate(List<? extends C95Item> items) {
			if (items == null) {
				items = Collections.emptyList();
			}
			BigDecimal total = null;
			return assignOutput(total, items);
		}
		
		protected BigDecimal assignOutput(BigDecimal total, List<? extends C95Item> items) {
			final MapperC<BigDecimal> thenArg = MapperC.<C95Item>of(items)
				.mapItem(i -> i.<BigDecimal>map("getV", c95Item -> c95Item.getV()));
			total = thenArg
				.<BigDecimal>reduce((a0, a1) -> MapperMaths.<BigDecimal, BigDecimal, BigDecimal>add(a0, a0)).get();
			
			return total;
		}
	}
}
