package test.wmwrapedge.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.util.Optional;
import javax.inject.Inject;
import test.wmwrapedge.Keyed;
import test.wmwrapedge.KeyedHolder;


@ImplementedBy(AliasKeyed.AliasKeyedDefault.class)
public abstract class AliasKeyed implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param kh 
	* @return r 
	*/
	public Keyed evaluate(KeyedHolder kh) {
		Keyed.KeyedBuilder rBuilder = doEvaluate(kh);
		
		final Keyed r;
		if (rBuilder == null) {
			r = null;
		} else {
			r = rBuilder.build();
			objectValidator.validate(Keyed.class, r);
		}
		
		return r;
	}

	protected abstract Keyed.KeyedBuilder doEvaluate(KeyedHolder kh);

	protected abstract MapperS<? extends Keyed> src(KeyedHolder kh);

	public static class AliasKeyedDefault extends AliasKeyed {
		@Override
		protected Keyed.KeyedBuilder doEvaluate(KeyedHolder kh) {
			Keyed.KeyedBuilder r = Keyed.builder();
			return assignOutput(r, kh);
		}
		
		protected Keyed.KeyedBuilder assignOutput(Keyed.KeyedBuilder r, KeyedHolder kh) {
			final Keyed.KeyedBuilder withMetaArgument = src(kh).get() == null ? null : src(kh).get().toBuilder();
			withMetaArgument.getOrCreateMeta().setExternalKey("the-key");
			r = toBuilder(withMetaArgument);
			
			return Optional.ofNullable(r)
				.map(o -> o.prune())
				.orElse(null);
		}
		
		@Override
		protected MapperS<? extends Keyed> src(KeyedHolder kh) {
			return MapperS.of(kh).<Keyed>map("getKeyed", keyedHolder -> keyedHolder.getKeyed());
		}
	}
}
