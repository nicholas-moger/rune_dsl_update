package chaos.s05.a2alias.functions;

import chaos.s05.a2alias.C5Item;
import chaos.s05.a2alias.h.C5Sub;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperListOfLists;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C5Chain.C5ChainDefault.class)
public abstract class C5Chain implements RosettaFunction {

	/**
	* @param items 
	* @return total 
	*/
	public BigDecimal evaluate(List<? extends C5Item> items) {
		BigDecimal total = doEvaluate(items);
		
		return total;
	}

	protected abstract BigDecimal doEvaluate(List<? extends C5Item> items);

	public static class C5ChainDefault extends C5Chain {
		@Override
		protected BigDecimal doEvaluate(List<? extends C5Item> items) {
			if (items == null) {
				items = Collections.emptyList();
			}
			BigDecimal total = null;
			return assignOutput(total, items);
		}
		
		protected BigDecimal assignOutput(BigDecimal total, List<? extends C5Item> items) {
			final MapperC<C5Item> thenArg0 = MapperC.<C5Item>of(items);
			final MapperListOfLists<C5Sub> thenArg1 = thenArg0
				.mapItemToList(item -> item.<C5Sub>mapC("getSub", c5Item -> c5Item.getSub()));
			final MapperC<C5Sub> thenArg2 = thenArg1
				.flattenList();
			final MapperListOfLists<BigDecimal> thenArg3 = thenArg2
				.mapItemToList(item -> item.<BigDecimal>mapC("getVals", c5Sub -> c5Sub.getVals()));
			final MapperC<BigDecimal> thenArg4 = thenArg3
				.flattenList();
			final MapperC<BigDecimal> thenArg5 = thenArg4
				.filterItemNullSafe(item -> greaterThanEquals(item, MapperS.of(BigDecimal.valueOf(0)), CardinalityOperator.All).get());
			total = thenArg5
				.sumBigDecimal().get();
			
			return total;
		}
	}
}
