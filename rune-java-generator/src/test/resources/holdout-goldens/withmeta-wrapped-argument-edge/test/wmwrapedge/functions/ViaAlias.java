package test.wmwrapedge.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.metafields.FieldWithMetaString;
import java.util.Optional;
import javax.inject.Inject;
import test.wmwrapedge.Holder;


@ImplementedBy(ViaAlias.ViaAliasDefault.class)
public abstract class ViaAlias implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param h 
	* @return c 
	*/
	public FieldWithMetaString evaluate(Holder h) {
		FieldWithMetaString.FieldWithMetaStringBuilder cBuilder = doEvaluate(h);
		
		final FieldWithMetaString c;
		if (cBuilder == null) {
			c = null;
		} else {
			c = cBuilder.build();
			objectValidator.validate(FieldWithMetaString.class, c);
		}
		
		return c;
	}

	protected abstract FieldWithMetaString.FieldWithMetaStringBuilder doEvaluate(Holder h);

	protected abstract MapperS<? extends FieldWithMetaString> src(Holder h);

	public static class ViaAliasDefault extends ViaAlias {
		@Override
		protected FieldWithMetaString.FieldWithMetaStringBuilder doEvaluate(Holder h) {
			FieldWithMetaString.FieldWithMetaStringBuilder c = FieldWithMetaString.builder();
			return assignOutput(c, h);
		}
		
		protected FieldWithMetaString.FieldWithMetaStringBuilder assignOutput(FieldWithMetaString.FieldWithMetaStringBuilder c, Holder h) {
			final FieldWithMetaString.FieldWithMetaStringBuilder withMetaArgument = src(h).get() == null ? null : src(h).get().toBuilder();
			withMetaArgument.getOrCreateMeta().setScheme("oracle-scheme");
			c = toBuilder(withMetaArgument);
			
			return Optional.ofNullable(c)
				.map(o -> o.prune())
				.orElse(null);
		}
		
		@Override
		protected MapperS<? extends FieldWithMetaString> src(Holder h) {
			return MapperS.of(h).<FieldWithMetaString>map("getCoded", holder -> holder.getCoded());
		}
	}
}
