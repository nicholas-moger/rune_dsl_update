package chaos.s30.a2alias.functions;

import chaos.s30.a2alias.C30Whole;
import chaos.s30.a2alias.h.C30Part;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.expression.MapperMaths;
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

@ImplementedBy(C30MultiThenAdd.C30MultiThenAddDefault.class)
public abstract class C30MultiThenAdd implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param n 
	* @param p 
	* @return w 
	*/
	public C30Whole evaluate(BigDecimal n, C30Part p) {
		C30Whole.C30WholeBuilder wBuilder = doEvaluate(n, p);
		
		final C30Whole w;
		if (wBuilder == null) {
			w = null;
		} else {
			w = wBuilder.build();
			objectValidator.validate(C30Whole.class, w);
		}
		
		return w;
	}

	protected abstract C30Whole.C30WholeBuilder doEvaluate(BigDecimal n, C30Part p);

	public static class C30MultiThenAddDefault extends C30MultiThenAdd {
		@Override
		protected C30Whole.C30WholeBuilder doEvaluate(BigDecimal n, C30Part p) {
			C30Whole.C30WholeBuilder w = C30Whole.builder();
			return assignOutput(w, n, p);
		}
		
		protected C30Whole.C30WholeBuilder assignOutput(C30Whole.C30WholeBuilder w, BigDecimal n, C30Part p) {
			w
				.setScores(MapperC.<BigDecimal>of(MapperS.of(n)).getMulti());
			
			w
				.addScores(MapperMaths.<BigDecimal, BigDecimal, BigDecimal>multiply(MapperS.of(n), MapperS.of(BigDecimal.valueOf(2))).getMulti());
			
			final List<BigDecimal> ifThenElseResult0;
			if (greaterThan(MapperS.of(n), MapperS.of(BigDecimal.valueOf(0)), CardinalityOperator.All).getOrDefault(false)) {
				ifThenElseResult0 = MapperC.<BigDecimal>of(MapperS.of(n), MapperMaths.<BigDecimal, BigDecimal, BigDecimal>add(MapperS.of(n), MapperS.of(BigDecimal.valueOf(1)))).getMulti();
			} else {
				ifThenElseResult0 = MapperC.<Integer>of(MapperS.of(0)).<BigDecimal>map("Type coercion", integer -> BigDecimal.valueOf(integer)).getMulti();
			}
			w
				.addScores(ifThenElseResult0);
			
			w
				.setParts(MapperC.<C30Part>of(MapperS.of(p), MapperS.of(C30Part.builder()
					.setPid("a")
					.build())).getMulti());
			
			w
				.addParts(MapperC.<C30Part>of(MapperS.of(C30Part.builder()
					.setPid("b")
					.build()), MapperS.of(C30Part.builder()
					.setPid("c")
					.build())).getMulti());
			
			final List<C30Part> ifThenElseResult1;
			if (greaterThan(MapperS.of(n), MapperS.of(BigDecimal.valueOf(1)), CardinalityOperator.All).getOrDefault(false)) {
				ifThenElseResult1 = p == null ? Collections.<C30Part>emptyList() : Collections.singletonList(p);
			} else {
				ifThenElseResult1 = Collections.<C30Part>emptyList();
			}
			w
				.addParts(ifThenElseResult1);
			
			w
				.setName("x");
			
			return Optional.ofNullable(w)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
