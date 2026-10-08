package test.fmeta002.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.metafields.FieldWithMetaString;
import java.util.Optional;
import javax.inject.Inject;


@ImplementedBy(A.ADefault.class)
public abstract class A implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param myInput 
	* @return result 
	*/
	public FieldWithMetaString evaluate(FieldWithMetaString myInput) {
		FieldWithMetaString.FieldWithMetaStringBuilder resultBuilder = doEvaluate(myInput);
		
		final FieldWithMetaString result;
		if (resultBuilder == null) {
			result = null;
		} else {
			result = resultBuilder.build();
			objectValidator.validate(FieldWithMetaString.class, result);
		}
		
		return result;
	}

	protected abstract FieldWithMetaString.FieldWithMetaStringBuilder doEvaluate(FieldWithMetaString myInput);

	public static class ADefault extends A {
		@Override
		protected FieldWithMetaString.FieldWithMetaStringBuilder doEvaluate(FieldWithMetaString myInput) {
			FieldWithMetaString.FieldWithMetaStringBuilder result = FieldWithMetaString.builder();
			return assignOutput(result, myInput);
		}
		
		protected FieldWithMetaString.FieldWithMetaStringBuilder assignOutput(FieldWithMetaString.FieldWithMetaStringBuilder result, FieldWithMetaString myInput) {
			result = toBuilder(myInput);
			
			return Optional.ofNullable(result)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
