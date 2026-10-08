package chaos.s29.a3hub.p2.functions;

import chaos.s29.a3hub.p1.C29Leaf;
import chaos.s29.a3hub.p2.C29Outer;
import chaos.s29.a3hub.p2.util.C29OuterDeepPathUtil;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperListOfLists;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C29Piped.C29PipedDefault.class)
public abstract class C29Piped implements RosettaFunction {
	
	// RosettaFunction dependencies
	//
	@Inject protected C29OuterDeepPathUtil c29OuterDeepPathUtil;

	/**
	* @param outers 
	* @return vs 
	*/
	public List<BigDecimal> evaluate(List<? extends C29Outer> outers) {
		List<BigDecimal> vs = doEvaluate(outers);
		
		return vs;
	}

	protected abstract List<BigDecimal> doEvaluate(List<? extends C29Outer> outers);

	protected abstract MapperC<BigDecimal> piped(List<? extends C29Outer> outers);

	public static class C29PipedDefault extends C29Piped {
		@Override
		protected List<BigDecimal> doEvaluate(List<? extends C29Outer> outers) {
			if (outers == null) {
				outers = Collections.emptyList();
			}
			List<BigDecimal> vs = new ArrayList<>();
			return assignOutput(vs, outers);
		}
		
		protected List<BigDecimal> assignOutput(List<BigDecimal> vs, List<? extends C29Outer> outers) {
			vs.addAll(piped(outers).getMulti());
			
			return vs;
		}
		
		@Override
		protected MapperC<BigDecimal> piped(List<? extends C29Outer> outers) {
			final MapperListOfLists<BigDecimal> thenArg = MapperC.<C29Outer>of(outers)
				.mapItemToList(o -> {
					final MapperC<C29Leaf> thenArg0 = o.<C29Leaf>mapC("chooseLeaves", c29Outer -> c29OuterDeepPathUtil.chooseLeaves(c29Outer));
					final MapperC<C29Leaf> thenArg1 = thenArg0
						.filterItemNullSafe(l -> greaterThan(l.<BigDecimal>map("getV", c29Leaf -> c29Leaf.getV()), MapperS.of(BigDecimal.valueOf(0)), CardinalityOperator.All).get());
					return thenArg1
						.mapItem(l2 -> l2.<BigDecimal>map("getV", c29Leaf -> c29Leaf.getV()));
				});
			return thenArg
				.flattenList();
		}
	}
}
