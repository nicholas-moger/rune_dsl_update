package chaos.s20.a2wild.functions;

import chaos.s20.a2wild.h.C20Leaf;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.MapperMaths;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;


@ImplementedBy(C20Spread.C20SpreadDefault.class)
public abstract class C20Spread implements RosettaFunction {

	/**
	* @param leaves 
	* @return spread 
	*/
	public BigDecimal evaluate(List<? extends C20Leaf> leaves) {
		BigDecimal spread = doEvaluate(leaves);
		
		return spread;
	}

	protected abstract BigDecimal doEvaluate(List<? extends C20Leaf> leaves);

	protected abstract MapperS<? extends C20Leaf> hiLeaf(List<? extends C20Leaf> leaves);

	protected abstract MapperS<? extends C20Leaf> loLeaf(List<? extends C20Leaf> leaves);

	protected abstract MapperC<? extends C20Leaf> ordered(List<? extends C20Leaf> leaves);

	protected abstract MapperS<? extends C20Leaf> backFirst(List<? extends C20Leaf> leaves);

	public static class C20SpreadDefault extends C20Spread {
		@Override
		protected BigDecimal doEvaluate(List<? extends C20Leaf> leaves) {
			if (leaves == null) {
				leaves = Collections.emptyList();
			}
			BigDecimal spread = null;
			return assignOutput(spread, leaves);
		}
		
		protected BigDecimal assignOutput(BigDecimal spread, List<? extends C20Leaf> leaves) {
			spread = MapperMaths.<BigDecimal, BigDecimal, BigDecimal>add(MapperMaths.<BigDecimal, BigDecimal, BigDecimal>add(MapperMaths.<BigDecimal, BigDecimal, BigDecimal>subtract(MapperS.of(hiLeaf(leaves).<BigDecimal>map("getV", c20Leaf -> c20Leaf.getV()).getOrDefault(BigDecimal.valueOf(0))), MapperS.of(loLeaf(leaves).<BigDecimal>map("getV", c20Leaf -> c20Leaf.getV()).getOrDefault(BigDecimal.valueOf(0)))), MapperMaths.<BigDecimal, BigDecimal, BigDecimal>multiply(MapperS.of(backFirst(leaves).<BigDecimal>map("getV", c20Leaf -> c20Leaf.getV()).getOrDefault(BigDecimal.valueOf(0))), MapperS.of(BigDecimal.valueOf(0)))), MapperMaths.<BigDecimal, BigDecimal, BigDecimal>multiply(MapperS.of(ordered(leaves)
				.last().<BigDecimal>map("getV", c20Leaf -> c20Leaf.getV()).getOrDefault(BigDecimal.valueOf(0))), MapperS.of(BigDecimal.valueOf(0)))).get();
			
			return spread;
		}
		
		@Override
		protected MapperS<? extends C20Leaf> hiLeaf(List<? extends C20Leaf> leaves) {
			return MapperC.<C20Leaf>of(leaves)
				.max(l -> l.<BigDecimal>map("getV", c20Leaf -> c20Leaf.getV()));
		}
		
		@Override
		protected MapperS<? extends C20Leaf> loLeaf(List<? extends C20Leaf> leaves) {
			return MapperC.<C20Leaf>of(leaves)
				.min(l -> l.<BigDecimal>map("getV", c20Leaf -> c20Leaf.getV()));
		}
		
		@Override
		protected MapperC<? extends C20Leaf> ordered(List<? extends C20Leaf> leaves) {
			return MapperC.<C20Leaf>of(leaves)
				.sort(l -> l.<BigDecimal>map("getV", c20Leaf -> c20Leaf.getV()));
		}
		
		@Override
		protected MapperS<? extends C20Leaf> backFirst(List<? extends C20Leaf> leaves) {
			final MapperC<? extends C20Leaf> thenArg0 = ordered(leaves);
			final MapperC<C20Leaf> thenArg1 = thenArg0
				.reverse();
			return thenArg1
				.first();
		}
	}
}
