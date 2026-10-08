package chaos.s09.a2wild.functions;

import chaos.s09.a2wild.C9Holder;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.metafields.FieldWithMetaString;
import java.util.Optional;
import javax.inject.Inject;


@ImplementedBy(C9Stamp.C9StampDefault.class)
public abstract class C9Stamp implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param h 
	* @return c 
	*/
	public FieldWithMetaString evaluate(C9Holder h) {
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

	protected abstract FieldWithMetaString.FieldWithMetaStringBuilder doEvaluate(C9Holder h);

	public static class C9StampDefault extends C9Stamp {
		@Override
		protected FieldWithMetaString.FieldWithMetaStringBuilder doEvaluate(C9Holder h) {
			FieldWithMetaString.FieldWithMetaStringBuilder c = FieldWithMetaString.builder();
			return assignOutput(c, h);
		}
		
		protected FieldWithMetaString.FieldWithMetaStringBuilder assignOutput(FieldWithMetaString.FieldWithMetaStringBuilder c, C9Holder h) {
			final FieldWithMetaString.FieldWithMetaStringBuilder withMetaArgument = MapperS.of(h).<FieldWithMetaString>map("getCoded", c9Holder -> c9Holder.getCoded()).get() == null ? null : MapperS.of(h).<FieldWithMetaString>map("getCoded", c9Holder -> c9Holder.getCoded()).get().toBuilder();
			withMetaArgument.getOrCreateMeta().setScheme("chaos-scheme");
			c = toBuilder(withMetaArgument);
			
			return Optional.ofNullable(c)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
