package chaos.s05.a1o1.functions;

import chaos.s05.a1o1.C5Item;
import chaos.s05.a1o1.C5Sub;
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

@ImplementedBy(C5Ctor.C5CtorDefault.class)
public abstract class C5Ctor implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param nm 
	* @param ns 
	* @return it 
	*/
	public C5Item evaluate(String nm, List<BigDecimal> ns) {
		C5Item.C5ItemBuilder itBuilder = doEvaluate(nm, ns);
		
		final C5Item it;
		if (itBuilder == null) {
			it = null;
		} else {
			it = itBuilder.build();
			objectValidator.validate(C5Item.class, it);
		}
		
		return it;
	}

	protected abstract C5Item.C5ItemBuilder doEvaluate(String nm, List<BigDecimal> ns);

	public static class C5CtorDefault extends C5Ctor {
		@Override
		protected C5Item.C5ItemBuilder doEvaluate(String nm, List<BigDecimal> ns) {
			if (ns == null) {
				ns = Collections.emptyList();
			}
			C5Item.C5ItemBuilder it = C5Item.builder();
			return assignOutput(it, nm, ns);
		}
		
		protected C5Item.C5ItemBuilder assignOutput(C5Item.C5ItemBuilder it, String nm, List<BigDecimal> ns) {
			it = toBuilder(C5Item.builder()
				.setOne(nm)
				.setOpt(MapperC.<BigDecimal>of(ns)
					.first().get())
				.setMany(MapperC.<BigDecimal>of(ns)
					.filterItemNullSafe(item -> greaterThan(item, MapperS.of(BigDecimal.valueOf(0)), CardinalityOperator.All).get()).getMulti())
				.setSub(MapperC.<C5Sub>of(MapperS.of(C5Sub.builder()
					.setVals(ns)
					.setName(nm)
					.build()), MapperS.of(C5Sub.builder()
					.setVals(MapperC.<Integer>of(MapperS.of(1), MapperS.of(2)).<BigDecimal>map("Type coercion", integer -> BigDecimal.valueOf(integer)).getMulti())
					.setName(MapperMaths.<String, String, String>add(MapperS.of(nm), MapperS.of("2")).get())
					.build())).getMulti())
				.build());
			
			return Optional.ofNullable(it)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
