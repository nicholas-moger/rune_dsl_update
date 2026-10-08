package test.wmwrap.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.metafields.FieldWithMetaString;
import com.rosetta.model.metafields.MetaFields;
import java.util.Optional;
import javax.inject.Inject;


@ImplementedBy(Literal.LiteralDefault.class)
public abstract class Literal implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @return c 
	*/
	public FieldWithMetaString evaluate() {
		FieldWithMetaString.FieldWithMetaStringBuilder cBuilder = doEvaluate();
		
		final FieldWithMetaString c;
		if (cBuilder == null) {
			c = null;
		} else {
			c = cBuilder.build();
			objectValidator.validate(FieldWithMetaString.class, c);
		}
		
		return c;
	}

	protected abstract FieldWithMetaString.FieldWithMetaStringBuilder doEvaluate();

	public static class LiteralDefault extends Literal {
		@Override
		protected FieldWithMetaString.FieldWithMetaStringBuilder doEvaluate() {
			FieldWithMetaString.FieldWithMetaStringBuilder c = FieldWithMetaString.builder();
			return assignOutput(c);
		}
		
		protected FieldWithMetaString.FieldWithMetaStringBuilder assignOutput(FieldWithMetaString.FieldWithMetaStringBuilder c) {
			final String withMetaArgument = "fixed";
			c = toBuilder(FieldWithMetaString.builder().setValue(withMetaArgument).setMeta(MetaFields.builder().setScheme("oracle-scheme")));
			
			return Optional.ofNullable(c)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
