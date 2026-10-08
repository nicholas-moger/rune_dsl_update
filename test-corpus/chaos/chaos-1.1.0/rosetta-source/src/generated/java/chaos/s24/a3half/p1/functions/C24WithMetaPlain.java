package chaos.s24.a3half.p1.functions;

import chaos.s24.a3half.p1.C24Ref;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.metafields.MetaFields;
import java.util.Optional;
import javax.inject.Inject;


@ImplementedBy(C24WithMetaPlain.C24WithMetaPlainDefault.class)
public abstract class C24WithMetaPlain implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param href 
	* @return r 
	*/
	public C24Ref evaluate(String href) {
		C24Ref.C24RefBuilder rBuilder = doEvaluate(href);
		
		final C24Ref r;
		if (rBuilder == null) {
			r = null;
		} else {
			r = rBuilder.build();
			objectValidator.validate(C24Ref.class, r);
		}
		
		return r;
	}

	protected abstract C24Ref.C24RefBuilder doEvaluate(String href);

	public static class C24WithMetaPlainDefault extends C24WithMetaPlain {
		@Override
		protected C24Ref.C24RefBuilder doEvaluate(String href) {
			C24Ref.C24RefBuilder r = C24Ref.builder();
			return assignOutput(r, href);
		}
		
		protected C24Ref.C24RefBuilder assignOutput(C24Ref.C24RefBuilder r, String href) {
			final C24Ref.C24RefBuilder withMetaArgument = null;
			r = toBuilder(C24Ref.builder().setValue(withMetaArgument).setMeta(MetaFields.builder().setExternalReference(href)));
			
			return Optional.ofNullable(r)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
