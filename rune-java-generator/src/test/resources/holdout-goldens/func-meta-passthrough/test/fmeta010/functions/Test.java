package test.fmeta010.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.metafields.FieldWithMetaString;
import java.util.Optional;
import javax.inject.Inject;
import test.fmeta010.Bar;


@ImplementedBy(Test.TestDefault.class)
public abstract class Test implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param myInput 
	* @return result 
	*/
	public Bar evaluate(FieldWithMetaString myInput) {
		Bar.BarBuilder resultBuilder = doEvaluate(myInput);
		
		final Bar result;
		if (resultBuilder == null) {
			result = null;
		} else {
			result = resultBuilder.build();
			objectValidator.validate(Bar.class, result);
		}
		
		return result;
	}

	protected abstract Bar.BarBuilder doEvaluate(FieldWithMetaString myInput);

	public static class TestDefault extends Test {
		@Override
		protected Bar.BarBuilder doEvaluate(FieldWithMetaString myInput) {
			Bar.BarBuilder result = Bar.builder();
			return assignOutput(result, myInput);
		}
		
		protected Bar.BarBuilder assignOutput(Bar.BarBuilder result, FieldWithMetaString myInput) {
			result
				.setB(myInput);
			
			return Optional.ofNullable(result)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
