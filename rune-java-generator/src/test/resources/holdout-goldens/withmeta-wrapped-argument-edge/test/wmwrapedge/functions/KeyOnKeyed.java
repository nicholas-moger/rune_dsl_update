package test.wmwrapedge.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import java.util.Optional;
import javax.inject.Inject;
import test.wmwrapedge.Keyed;


@ImplementedBy(KeyOnKeyed.KeyOnKeyedDefault.class)
public abstract class KeyOnKeyed implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param k 
	* @return r 
	*/
	public Keyed evaluate(Keyed k) {
		Keyed.KeyedBuilder rBuilder = doEvaluate(k);
		
		final Keyed r;
		if (rBuilder == null) {
			r = null;
		} else {
			r = rBuilder.build();
			objectValidator.validate(Keyed.class, r);
		}
		
		return r;
	}

	protected abstract Keyed.KeyedBuilder doEvaluate(Keyed k);

	public static class KeyOnKeyedDefault extends KeyOnKeyed {
		@Override
		protected Keyed.KeyedBuilder doEvaluate(Keyed k) {
			Keyed.KeyedBuilder r = Keyed.builder();
			return assignOutput(r, k);
		}
		
		protected Keyed.KeyedBuilder assignOutput(Keyed.KeyedBuilder r, Keyed k) {
			final Keyed.KeyedBuilder withMetaArgument = k == null ? null : k.toBuilder();
			withMetaArgument.getOrCreateMeta().setExternalKey("the-key");
			r = toBuilder(withMetaArgument);
			
			return Optional.ofNullable(r)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
