package holdout.voidmetaoutput.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.metafields.FieldWithMetaVoid;
import holdout.voidmetaoutput.Holder;
import java.util.Optional;
import javax.inject.Inject;


@ImplementedBy(SetMetaVoid.SetMetaVoidDefault.class)
public abstract class SetMetaVoid implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param h 
	* @return t 
	*/
	public FieldWithMetaVoid evaluate(Holder h) {
		FieldWithMetaVoid.FieldWithMetaVoidBuilder tBuilder = doEvaluate(h);
		
		final FieldWithMetaVoid t;
		if (tBuilder == null) {
			t = null;
		} else {
			t = tBuilder.build();
			objectValidator.validate(FieldWithMetaVoid.class, t);
		}
		
		return t;
	}

	protected abstract FieldWithMetaVoid.FieldWithMetaVoidBuilder doEvaluate(Holder h);

	public static class SetMetaVoidDefault extends SetMetaVoid {
		@Override
		protected FieldWithMetaVoid.FieldWithMetaVoidBuilder doEvaluate(Holder h) {
			FieldWithMetaVoid.FieldWithMetaVoidBuilder t = FieldWithMetaVoid.builder();
			return assignOutput(t, h);
		}
		
		protected FieldWithMetaVoid.FieldWithMetaVoidBuilder assignOutput(FieldWithMetaVoid.FieldWithMetaVoidBuilder t, Holder h) {
			t = toBuilder(FieldWithMetaVoid.builder().build());
			
			return Optional.ofNullable(t)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
