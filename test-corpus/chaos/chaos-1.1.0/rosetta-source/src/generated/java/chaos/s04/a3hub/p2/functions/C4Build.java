package chaos.s04.a3hub.p2.functions;

import chaos.s04.a3hub.p1.C4Pair;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.ConditionValidator;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C4Build.C4BuildDefault.class)
public abstract class C4Build implements RosettaFunction {
	
	@Inject protected ConditionValidator conditionValidator;
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param xs 
	* @return p 
	*/
	public C4Pair evaluate(List<BigDecimal> xs) {
		// pre-conditions
		conditionValidator.validate(() -> greaterThanEquals(MapperC.<BigDecimal>of(xs), MapperS.of(BigDecimal.valueOf(0)), CardinalityOperator.All),
			"");
		
		C4Pair.C4PairBuilder pBuilder = doEvaluate(xs);
		
		final C4Pair p;
		if (pBuilder == null) {
			p = null;
		} else {
			p = pBuilder.build();
			objectValidator.validate(C4Pair.class, p);
		}
		
		// post-conditions
		conditionValidator.validate(() -> exists(MapperS.of(p).<BigDecimal>map("getLeft", c4Pair -> c4Pair.getLeft())).andNullSafe(exists(MapperS.of(p).<BigDecimal>map("getRight", c4Pair -> c4Pair.getRight()))),
			"");
		
		return p;
	}

	protected abstract C4Pair.C4PairBuilder doEvaluate(List<BigDecimal> xs);

	protected abstract MapperS<BigDecimal> lo(List<BigDecimal> xs);

	protected abstract MapperS<BigDecimal> hiV(List<BigDecimal> xs);

	public static class C4BuildDefault extends C4Build {
		@Override
		protected C4Pair.C4PairBuilder doEvaluate(List<BigDecimal> xs) {
			if (xs == null) {
				xs = Collections.emptyList();
			}
			C4Pair.C4PairBuilder p = C4Pair.builder();
			return assignOutput(p, xs);
		}
		
		protected C4Pair.C4PairBuilder assignOutput(C4Pair.C4PairBuilder p, List<BigDecimal> xs) {
			p = toBuilder(C4Pair.builder()
				.setLeft(lo(xs).get())
				.setRight(hiV(xs).get())
				.build());
			
			return Optional.ofNullable(p)
				.map(o -> o.prune())
				.orElse(null);
		}
		
		@Override
		protected MapperS<BigDecimal> lo(List<BigDecimal> xs) {
			return MapperS.of(MapperC.<BigDecimal>of(xs)
				.first().getOrDefault(BigDecimal.valueOf(0)));
		}
		
		@Override
		protected MapperS<BigDecimal> hiV(List<BigDecimal> xs) {
			return MapperS.of(MapperC.<BigDecimal>of(xs)
				.last().getOrDefault(BigDecimal.valueOf(0)));
		}
	}
}
