package test.wmwrap.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.metafields.FieldWithMetaString;
import com.rosetta.model.metafields.MetaFields;
import java.util.Optional;
import javax.inject.Inject;
import test.wmwrap.Holder;


@ImplementedBy(Plain.PlainDefault.class)
public abstract class Plain implements RosettaFunction {
	
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

	public static class PlainDefault extends Plain {
		@Override
		protected FieldWithMetaString.FieldWithMetaStringBuilder doEvaluate(Holder h) {
			FieldWithMetaString.FieldWithMetaStringBuilder c = FieldWithMetaString.builder();
			return assignOutput(c, h);
		}
		
		protected FieldWithMetaString.FieldWithMetaStringBuilder assignOutput(FieldWithMetaString.FieldWithMetaStringBuilder c, Holder h) {
			final String withMetaArgument = MapperS.of(h).<String>map("getPlain", holder -> holder.getPlain()).get();
			c = toBuilder(FieldWithMetaString.builder().setValue(withMetaArgument).setMeta(MetaFields.builder().setScheme("oracle-scheme")));
			
			return Optional.ofNullable(c)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
