package chaos.s19.a1o2.functions;

import chaos.s19.a1o2.C19Part;
import chaos.s19.a1o2.C19Whole;
import chaos.s19.a1o2.metafields.ReferenceWithMetaC19Part;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.MapperMaths;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import java.util.Optional;
import javax.inject.Inject;


@ImplementedBy(C19Make.C19MakeDefault.class)
public abstract class C19Make implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param nm 
	* @param p 
	* @return w 
	*/
	public C19Whole evaluate(String nm, C19Part p) {
		C19Whole.C19WholeBuilder wBuilder = doEvaluate(nm, p);
		
		final C19Whole w;
		if (wBuilder == null) {
			w = null;
		} else {
			w = wBuilder.build();
			objectValidator.validate(C19Whole.class, w);
		}
		
		return w;
	}

	protected abstract C19Whole.C19WholeBuilder doEvaluate(String nm, C19Part p);

	public static class C19MakeDefault extends C19Make {
		@Override
		protected C19Whole.C19WholeBuilder doEvaluate(String nm, C19Part p) {
			C19Whole.C19WholeBuilder w = C19Whole.builder();
			return assignOutput(w, nm, p);
		}
		
		protected C19Whole.C19WholeBuilder assignOutput(C19Whole.C19WholeBuilder w, String nm, C19Part p) {
			w = toBuilder(C19Whole.builder()
				.setName(nm)
				.setOpt(null)
				.setScores(MapperC.<Integer>of(MapperS.of(1), MapperS.of(2), MapperS.of(3)).<BigDecimal>map("Type coercion", integer -> BigDecimal.valueOf(integer)).getMulti())
				.setPart(C19Part.builder()
					.setPid(MapperMaths.<String, String, String>add(MapperS.of(nm), MapperS.of("-p")).get())
					.build())
				.setPartRef(ReferenceWithMetaC19Part.builder()
					.setGlobalReference(Optional.ofNullable(p)
						.map(r -> r.getMeta())
						.map(m -> m.getGlobalKey())
						.orElse(null))
					.setExternalReference(Optional.ofNullable(p)
						.map(r -> r.getMeta())
						.map(m -> m.getExternalKey())
						.orElse(null))
					.build())
				.setParts(MapperC.<C19Part>of(MapperS.of(p), MapperS.of(C19Part.builder()
					.setPid("fixed")
					.build())).getMulti())
				.build());
			
			return Optional.ofNullable(w)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
