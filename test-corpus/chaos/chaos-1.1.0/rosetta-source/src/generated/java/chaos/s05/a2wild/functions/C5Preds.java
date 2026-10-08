package chaos.s05.a2wild.functions;

import chaos.s05.a2wild.C5Item;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C5Preds.C5PredsDefault.class)
public abstract class C5Preds implements RosettaFunction {

	/**
	* @param items 
	* @param probe 
	* @return ok 
	*/
	public Boolean evaluate(List<? extends C5Item> items, BigDecimal probe) {
		Boolean ok = doEvaluate(items, probe);
		
		return ok;
	}

	protected abstract Boolean doEvaluate(List<? extends C5Item> items, BigDecimal probe);

	protected abstract MapperC<BigDecimal> pool(List<? extends C5Item> items, BigDecimal probe);

	protected abstract MapperC<String> ones(List<? extends C5Item> items, BigDecimal probe);

	protected abstract MapperS<String> joined(List<? extends C5Item> items, BigDecimal probe);

	public static class C5PredsDefault extends C5Preds {
		@Override
		protected Boolean doEvaluate(List<? extends C5Item> items, BigDecimal probe) {
			if (items == null) {
				items = Collections.emptyList();
			}
			Boolean ok = null;
			return assignOutput(ok, items, probe);
		}
		
		protected Boolean assignOutput(Boolean ok, List<? extends C5Item> items, BigDecimal probe) {
			final MapperC<C5Item> thenArg0 = MapperC.<C5Item>of(items);
			final MapperC<C5Item> thenArg1 = thenArg0
				.filterItemNullSafe(item -> exists(item.<BigDecimal>map("getOpt", c5Item -> c5Item.getOpt())).get());
			ok = contains(pool(items, probe), MapperS.of(probe)).andNullSafe(contains(ones(items, probe), MapperS.of("x"))).andNullSafe(areEqual(MapperS.of(probe), MapperS.of(pool(items, probe)
				.first().getOrDefault(BigDecimal.valueOf(0))), CardinalityOperator.All)).andNullSafe(greaterThanEquals(MapperS.of(thenArg1.resultCount()), MapperS.of(0), CardinalityOperator.All)).andNullSafe(exists(joined(items, probe))).get();
			
			return ok;
		}
		
		@Override
		protected MapperC<BigDecimal> pool(List<? extends C5Item> items, BigDecimal probe) {
			return MapperC.<C5Item>of(items)
				.mapItem(item -> item.<BigDecimal>map("getOpt", c5Item -> c5Item.getOpt()));
		}
		
		@Override
		protected MapperC<String> ones(List<? extends C5Item> items, BigDecimal probe) {
			return MapperC.<C5Item>of(items)
				.mapItem(item -> item.<String>map("getOne", c5Item -> c5Item.getOne()));
		}
		
		@Override
		protected MapperS<String> joined(List<? extends C5Item> items, BigDecimal probe) {
			final MapperC<String> thenArg = ones(items, probe);
			return thenArg.join(MapperS.of(", "));
		}
	}
}
