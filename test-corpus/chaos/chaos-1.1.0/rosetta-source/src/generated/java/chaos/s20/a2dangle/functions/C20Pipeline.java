package chaos.s20.a2dangle.functions;

import chaos.s20.a2dangle.C20Branch;
import chaos.s20.a2dangle.C20Trunk;
import chaos.s20.a2dangle.C20Twig;
import chaos.s20.a2dangle.h.C20Leaf;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.expression.MapperMaths;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperListOfLists;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C20Pipeline.C20PipelineDefault.class)
public abstract class C20Pipeline implements RosettaFunction {

	/**
	* @param trunks 
	* @return total 
	*/
	public BigDecimal evaluate(List<? extends C20Trunk> trunks) {
		BigDecimal total = doEvaluate(trunks);
		
		return total;
	}

	protected abstract BigDecimal doEvaluate(List<? extends C20Trunk> trunks);

	public static class C20PipelineDefault extends C20Pipeline {
		@Override
		protected BigDecimal doEvaluate(List<? extends C20Trunk> trunks) {
			if (trunks == null) {
				trunks = Collections.emptyList();
			}
			BigDecimal total = null;
			return assignOutput(total, trunks);
		}
		
		protected BigDecimal assignOutput(BigDecimal total, List<? extends C20Trunk> trunks) {
			final MapperC<C20Trunk> thenArg0 = MapperC.<C20Trunk>of(trunks);
			final MapperListOfLists<C20Branch> thenArg1 = thenArg0
				.mapItemToList(item -> item.<C20Branch>mapC("getBranches", c20Trunk -> c20Trunk.getBranches()));
			final MapperC<C20Branch> thenArg2 = thenArg1
				.flattenList();
			final MapperListOfLists<C20Twig> thenArg3 = thenArg2
				.mapItemToList(item -> item.<C20Twig>mapC("getTwigs", c20Branch -> c20Branch.getTwigs()));
			final MapperC<C20Twig> thenArg4 = thenArg3
				.flattenList();
			final MapperListOfLists<C20Leaf> thenArg5 = thenArg4
				.mapItemToList(item -> item.<C20Leaf>mapC("getLeaves", c20Twig -> c20Twig.getLeaves()));
			final MapperC<C20Leaf> thenArg6 = thenArg5
				.flattenList();
			final MapperC<BigDecimal> thenArg7 = thenArg6
				.mapItem(item -> item.<BigDecimal>map("getV", c20Leaf -> c20Leaf.getV()));
			final MapperC<BigDecimal> thenArg8 = thenArg7
				.filterItemNullSafe(item -> greaterThanEquals(item, MapperS.of(BigDecimal.valueOf(0)), CardinalityOperator.All).get());
			final MapperC<BigDecimal> thenArg9 = distinct(thenArg8);
			final MapperC<BigDecimal> thenArg10 = thenArg9
				.sort();
			total = thenArg10
				.<BigDecimal>reduce((a, b) -> MapperMaths.<BigDecimal, BigDecimal, BigDecimal>add(a, b)).get();
			
			return total;
		}
	}
}
