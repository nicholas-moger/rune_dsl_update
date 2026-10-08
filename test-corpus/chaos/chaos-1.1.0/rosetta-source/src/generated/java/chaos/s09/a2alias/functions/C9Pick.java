package chaos.s09.a2alias.functions;

import chaos.s09.a2alias.C9Holder;
import chaos.s09.a2alias.C9Keyed;
import chaos.s09.a2alias.metafields.ReferenceWithMetaC9Keyed;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.util.Optional;
import javax.inject.Inject;


@ImplementedBy(C9Pick.C9PickDefault.class)
public abstract class C9Pick implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param h 
	* @return k 
	*/
	public C9Keyed evaluate(C9Holder h) {
		C9Keyed.C9KeyedBuilder kBuilder = doEvaluate(h);
		
		final C9Keyed k;
		if (kBuilder == null) {
			k = null;
		} else {
			k = kBuilder.build();
			objectValidator.validate(C9Keyed.class, k);
		}
		
		return k;
	}

	protected abstract C9Keyed.C9KeyedBuilder doEvaluate(C9Holder h);

	public static class C9PickDefault extends C9Pick {
		@Override
		protected C9Keyed.C9KeyedBuilder doEvaluate(C9Holder h) {
			C9Keyed.C9KeyedBuilder k = C9Keyed.builder();
			return assignOutput(k, h);
		}
		
		protected C9Keyed.C9KeyedBuilder assignOutput(C9Keyed.C9KeyedBuilder k, C9Holder h) {
			final ReferenceWithMetaC9Keyed referenceWithMetaC9Keyed = MapperS.of(h).<ReferenceWithMetaC9Keyed>map("getByRef", c9Holder -> c9Holder.getByRef()).get();
			if (referenceWithMetaC9Keyed == null) {
				k = null;
			} else {
				k = toBuilder(referenceWithMetaC9Keyed.getValue());
			}
			
			return Optional.ofNullable(k)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
