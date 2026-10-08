package chaos.s19.base.functions;

import chaos.s19.base.C19Part;
import chaos.s19.base.C19Whole;
import chaos.s19.base.metafields.ReferenceWithMetaC19Part;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import java.util.Optional;
import javax.inject.Inject;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C19Pick.C19PickDefault.class)
public abstract class C19Pick implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param n 
	* @param p 
	* @return w 
	*/
	public C19Whole evaluate(BigDecimal n, C19Part p) {
		C19Whole.C19WholeBuilder wBuilder = doEvaluate(n, p);
		
		final C19Whole w;
		if (wBuilder == null) {
			w = null;
		} else {
			w = wBuilder.build();
			objectValidator.validate(C19Whole.class, w);
		}
		
		return w;
	}

	protected abstract C19Whole.C19WholeBuilder doEvaluate(BigDecimal n, C19Part p);

	public static class C19PickDefault extends C19Pick {
		@Override
		protected C19Whole.C19WholeBuilder doEvaluate(BigDecimal n, C19Part p) {
			C19Whole.C19WholeBuilder w = C19Whole.builder();
			return assignOutput(w, n, p);
		}
		
		protected C19Whole.C19WholeBuilder assignOutput(C19Whole.C19WholeBuilder w, BigDecimal n, C19Part p) {
			String ifThenElseResult0 = "neg";
			if (greaterThan(MapperS.of(n), MapperS.of(BigDecimal.valueOf(0)), CardinalityOperator.All).getOrDefault(false)) {
				ifThenElseResult0 = "pos";
			}
			BigDecimal ifThenElseResult1 = null;
			if (greaterThan(MapperS.of(n), MapperS.of(BigDecimal.valueOf(100)), CardinalityOperator.All).getOrDefault(false)) {
				ifThenElseResult1 = n;
			}
			w = toBuilder(C19Whole.builder()
				.setName(ifThenElseResult0)
				.setOpt(ifThenElseResult1)
				.build(), () -> C19Whole.builder());
			
			w
				.setPartRef(ReferenceWithMetaC19Part.builder()
					.setGlobalReference(Optional.ofNullable(p)
						.map(r -> r.getMeta())
						.map(m -> m.getGlobalKey())
						.orElse(null))
					.setExternalReference(Optional.ofNullable(p)
						.map(r -> r.getMeta())
						.map(m -> m.getExternalKey())
						.orElse(null))
					.build()
				);
			
			return Optional.ofNullable(w)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
