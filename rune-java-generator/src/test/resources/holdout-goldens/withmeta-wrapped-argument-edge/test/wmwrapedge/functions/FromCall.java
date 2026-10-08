package test.wmwrapedge.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.metafields.FieldWithMetaString;
import java.util.Optional;
import javax.inject.Inject;
import test.wmwrapedge.Holder;


@ImplementedBy(FromCall.FromCallDefault.class)
public abstract class FromCall implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;
	
	// RosettaFunction dependencies
	//
	@Inject protected Wrapped wrapped;

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

	public static class FromCallDefault extends FromCall {
		@Override
		protected FieldWithMetaString.FieldWithMetaStringBuilder doEvaluate(Holder h) {
			FieldWithMetaString.FieldWithMetaStringBuilder c = FieldWithMetaString.builder();
			return assignOutput(c, h);
		}
		
		protected FieldWithMetaString.FieldWithMetaStringBuilder assignOutput(FieldWithMetaString.FieldWithMetaStringBuilder c, Holder h) {
			final FieldWithMetaString.FieldWithMetaStringBuilder withMetaArgument = wrapped.evaluate(h) == null ? null : wrapped.evaluate(h).toBuilder();
			withMetaArgument.getOrCreateMeta().setScheme("other-scheme");
			c = toBuilder(withMetaArgument);
			
			return Optional.ofNullable(c)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
