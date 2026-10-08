package chaos.s26.a1o2.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.expression.MapperMaths;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C26Lit.C26LitDefault.class)
public abstract class C26Lit implements RosettaFunction {

	/**
	* @param raws 
	* @return n 
	*/
	public BigDecimal evaluate(List<String> raws) {
		BigDecimal n = doEvaluate(raws);
		
		return n;
	}

	protected abstract BigDecimal doEvaluate(List<String> raws);

	protected abstract MapperC<BigDecimal> nums(List<String> raws);

	protected abstract MapperC<Boolean> flags(List<String> raws);

	public static class C26LitDefault extends C26Lit {
		@Override
		protected BigDecimal doEvaluate(List<String> raws) {
			if (raws == null) {
				raws = Collections.emptyList();
			}
			BigDecimal n = null;
			return assignOutput(n, raws);
		}
		
		protected BigDecimal assignOutput(BigDecimal n, List<String> raws) {
			final MapperC<BigDecimal> thenArg0 = nums(raws);
			final MapperC<Boolean> thenArg1 = flags(raws);
			final MapperC<Boolean> thenArg2 = thenArg1
				.filterItemNullSafe(item -> areEqual(item, MapperS.of(true), CardinalityOperator.All).get());
			n = MapperMaths.<BigDecimal, BigDecimal, BigDecimal>add(thenArg0
				.sumBigDecimal(), MapperS.of(thenArg2.resultCount()).<BigDecimal>map("Type coercion", integer -> integer == null ? null : BigDecimal.valueOf(integer))).get();
			
			return n;
		}
		
		@Override
		protected MapperC<BigDecimal> nums(List<String> raws) {
			return MapperC.<String>of(raws)
				.mapItem(item -> {
					if (item.get() == null) {
						return MapperS.<BigDecimal>ofNull();
					}
					if (areEqual(item, MapperS.of("a"), CardinalityOperator.All).get()) {
						return MapperS.of(new BigDecimal("1.5"));
					}
					if (areEqual(item, MapperS.of("b"), CardinalityOperator.All).get()) {
						return MapperS.of(new BigDecimal("2.25"));
					}
					return MapperS.of(new BigDecimal("0.5"));
				});
		}
		
		@Override
		protected MapperC<Boolean> flags(List<String> raws) {
			return MapperC.<String>of(raws)
				.mapItem(item -> {
					if (item.get() == null) {
						return MapperS.<Boolean>ofNull();
					}
					if (areEqual(item, MapperS.of("a"), CardinalityOperator.All).get()) {
						return MapperS.of(true);
					}
					if (areEqual(item, MapperS.of("b"), CardinalityOperator.All).get()) {
						return MapperS.of(false);
					}
					return MapperS.of(false);
				});
		}
	}
}
