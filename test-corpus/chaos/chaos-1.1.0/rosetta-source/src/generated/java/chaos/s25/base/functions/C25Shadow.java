package chaos.s25.base.functions;

import chaos.s25.base.C25Paths;
import chaos.s25.base.C25Sub;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.MapperMaths;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C25Shadow.C25ShadowDefault.class)
public abstract class C25Shadow implements RosettaFunction {

	/**
	* @param ps 
	* @return n 
	*/
	public BigDecimal evaluate(List<? extends C25Paths> ps) {
		BigDecimal n = doEvaluate(ps);
		
		return n;
	}

	protected abstract BigDecimal doEvaluate(List<? extends C25Paths> ps);

	protected abstract MapperC<String> bySub(List<? extends C25Paths> ps);

	protected abstract MapperC<? extends C25Paths> sorted(List<? extends C25Paths> ps);

	protected abstract MapperS<? extends C25Paths> hi(List<? extends C25Paths> ps);

	protected abstract MapperS<? extends C25Paths> lo(List<? extends C25Paths> ps);

	protected abstract MapperC<? extends C25Paths> kept(List<? extends C25Paths> ps);

	protected abstract MapperS<Integer> total(List<? extends C25Paths> ps);

	protected abstract MapperC<Integer> mixed(List<? extends C25Paths> ps);

	public static class C25ShadowDefault extends C25Shadow {
		@Override
		protected BigDecimal doEvaluate(List<? extends C25Paths> ps) {
			if (ps == null) {
				ps = Collections.emptyList();
			}
			BigDecimal n = null;
			return assignOutput(n, ps);
		}
		
		protected BigDecimal assignOutput(BigDecimal n, List<? extends C25Paths> ps) {
			final MapperC<Integer> thenArg = mixed(ps);
			final MapperS<Integer> ifThenElseResult0;
			if (exists(hi(ps)).getOrDefault(false)) {
				ifThenElseResult0 = MapperS.of(1);
			} else {
				ifThenElseResult0 = MapperS.of(0);
			}
			final MapperS<Integer> ifThenElseResult1;
			if (exists(lo(ps)).getOrDefault(false)) {
				ifThenElseResult1 = MapperS.of(1);
			} else {
				ifThenElseResult1 = MapperS.of(0);
			}
			final Integer integer = MapperMaths.<Integer, Integer, Integer>add(MapperMaths.<Integer, Integer, Integer>add(MapperMaths.<Integer, Integer, Integer>add(MapperMaths.<Integer, Integer, Integer>add(MapperMaths.<Integer, Integer, Integer>add(MapperMaths.<Integer, Integer, Integer>add(MapperS.of(bySub(ps).resultCount()), MapperS.of(sorted(ps).resultCount())), MapperS.of(kept(ps).resultCount())), total(ps)), thenArg
				.sumInteger()), ifThenElseResult0), ifThenElseResult1).get();
			if (integer == null) {
				n = null;
			} else {
				n = BigDecimal.valueOf(integer);
			}
			
			return n;
		}
		
		@Override
		protected MapperC<String> bySub(List<? extends C25Paths> ps) {
			return MapperC.<C25Paths>of(ps)
				.mapItem(sub -> sub.<C25Sub>map("getSub", c25Paths -> c25Paths.getSub()).<String>map("getSname", c25Sub -> c25Sub.getSname()));
		}
		
		@Override
		protected MapperC<? extends C25Paths> sorted(List<? extends C25Paths> ps) {
			return MapperC.<C25Paths>of(ps)
				.sort(sub -> sub.<String>map("getP", c25Paths -> c25Paths.getP()));
		}
		
		@Override
		protected MapperS<? extends C25Paths> hi(List<? extends C25Paths> ps) {
			return MapperC.<C25Paths>of(ps)
				.max(sub -> sub.<String>map("getQ", c25Paths -> c25Paths.getQ()));
		}
		
		@Override
		protected MapperS<? extends C25Paths> lo(List<? extends C25Paths> ps) {
			return MapperC.<C25Paths>of(ps)
				.min(sub -> sub.<String>map("getQ", c25Paths -> c25Paths.getQ()));
		}
		
		@Override
		protected MapperC<? extends C25Paths> kept(List<? extends C25Paths> ps) {
			return MapperC.<C25Paths>of(ps)
				.filterItemNullSafe(sub -> exists(sub.<String>map("getP", c25Paths -> c25Paths.getP())).get());
		}
		
		@Override
		protected MapperS<Integer> total(List<? extends C25Paths> ps) {
			final MapperC<Integer> thenArg = MapperC.<C25Paths>of(ps)
				.mapItem(sub -> MapperS.of(sub.<C25Sub>mapC("getSubs", c25Paths -> c25Paths.getSubs()).resultCount()));
			return thenArg
				.<Integer>reduce((a, b) -> MapperMaths.<Integer, Integer, Integer>add(a, b));
		}
		
		@Override
		protected MapperC<Integer> mixed(List<? extends C25Paths> ps) {
			return MapperC.<C25Paths>of(ps)
				.mapItem(x -> {
					final MapperC<C25Sub> thenArg0 = x.<C25Sub>mapC("getSubs", c25Paths -> c25Paths.getSubs());
					final MapperC<C25Sub> thenArg1 = thenArg0
						.filterItemNullSafe(item -> exists(item.<BigDecimal>mapC("getSubs", c25Sub -> c25Sub.getSubs())).get());
					return MapperS.of(thenArg1.resultCount());
				});
		}
	}
}
