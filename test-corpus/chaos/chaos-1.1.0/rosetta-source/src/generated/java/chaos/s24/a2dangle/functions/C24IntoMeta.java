package chaos.s24.a2dangle.functions;

import chaos.s24.a2dangle.C24Carrier;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.metafields.FieldWithMetaVoid;
import java.util.Optional;
import javax.inject.Inject;


@ImplementedBy(C24IntoMeta.C24IntoMetaDefault.class)
public abstract class C24IntoMeta implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param c 
	* @return t 
	*/
	public FieldWithMetaVoid evaluate(C24Carrier c) {
		FieldWithMetaVoid.FieldWithMetaVoidBuilder tBuilder = doEvaluate(c);
		
		final FieldWithMetaVoid t;
		if (tBuilder == null) {
			t = null;
		} else {
			t = tBuilder.build();
			objectValidator.validate(FieldWithMetaVoid.class, t);
		}
		
		return t;
	}

	protected abstract FieldWithMetaVoid.FieldWithMetaVoidBuilder doEvaluate(C24Carrier c);

	public static class C24IntoMetaDefault extends C24IntoMeta {
		@Override
		protected FieldWithMetaVoid.FieldWithMetaVoidBuilder doEvaluate(C24Carrier c) {
			FieldWithMetaVoid.FieldWithMetaVoidBuilder t = FieldWithMetaVoid.builder();
			return assignOutput(t, c);
		}
		
		protected FieldWithMetaVoid.FieldWithMetaVoidBuilder assignOutput(FieldWithMetaVoid.FieldWithMetaVoidBuilder t, C24Carrier c) {
			t = toBuilder(FieldWithMetaVoid.builder().build());
			
			return Optional.ofNullable(t)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
