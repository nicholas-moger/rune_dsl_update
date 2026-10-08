package chaos.s04.a1o2.functions;

import chaos.s04.a1o2.C4Pair;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.MapperMaths;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import java.util.Optional;
import javax.inject.Inject;


@ImplementedBy(C4Seg.C4SegDefault.class)
public abstract class C4Seg implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param v 
	* @return p 
	*/
	public C4Pair evaluate(BigDecimal v) {
		C4Pair.C4PairBuilder pBuilder = doEvaluate(v);
		
		final C4Pair p;
		if (pBuilder == null) {
			p = null;
		} else {
			p = pBuilder.build();
			objectValidator.validate(C4Pair.class, p);
		}
		
		return p;
	}

	protected abstract C4Pair.C4PairBuilder doEvaluate(BigDecimal v);

	public static class C4SegDefault extends C4Seg {
		@Override
		protected C4Pair.C4PairBuilder doEvaluate(BigDecimal v) {
			C4Pair.C4PairBuilder p = C4Pair.builder();
			return assignOutput(p, v);
		}
		
		protected C4Pair.C4PairBuilder assignOutput(C4Pair.C4PairBuilder p, BigDecimal v) {
			p
				.setLeft(v);
			
			p
				.setRight(MapperMaths.<BigDecimal, BigDecimal, BigDecimal>add(MapperS.of(v), MapperS.of(BigDecimal.valueOf(1))).get());
			
			return Optional.ofNullable(p)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
