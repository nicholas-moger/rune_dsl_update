package test.wmwrapedge.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import java.util.Optional;
import javax.inject.Inject;
import test.wmwrapedge.Keyed;
import test.wmwrapedge.metafields.ReferenceWithMetaKeyed;


@ImplementedBy(RefOnKeyed.RefOnKeyedDefault.class)
public abstract class RefOnKeyed implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param k 
	* @return r 
	*/
	public ReferenceWithMetaKeyed evaluate(Keyed k) {
		ReferenceWithMetaKeyed.ReferenceWithMetaKeyedBuilder rBuilder = doEvaluate(k);
		
		final ReferenceWithMetaKeyed r;
		if (rBuilder == null) {
			r = null;
		} else {
			r = rBuilder.build();
			objectValidator.validate(ReferenceWithMetaKeyed.class, r);
		}
		
		return r;
	}

	protected abstract ReferenceWithMetaKeyed.ReferenceWithMetaKeyedBuilder doEvaluate(Keyed k);

	public static class RefOnKeyedDefault extends RefOnKeyed {
		@Override
		protected ReferenceWithMetaKeyed.ReferenceWithMetaKeyedBuilder doEvaluate(Keyed k) {
			ReferenceWithMetaKeyed.ReferenceWithMetaKeyedBuilder r = ReferenceWithMetaKeyed.builder();
			return assignOutput(r, k);
		}
		
		protected ReferenceWithMetaKeyed.ReferenceWithMetaKeyedBuilder assignOutput(ReferenceWithMetaKeyed.ReferenceWithMetaKeyedBuilder r, Keyed k) {
			r = toBuilder(ReferenceWithMetaKeyed.builder().setValue(k == null ? null : k.toBuilder()).setExternalReference("the-ref").build());
			
			return Optional.ofNullable(r)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
