package test.fmeta022.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.metafields.ReferenceWithMetaString;
import java.util.Collections;
import java.util.Optional;
import javax.inject.Inject;
import test.fmeta022.A;


@ImplementedBy(Test.TestDefault.class)
public abstract class Test implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @return result 
	*/
	public A evaluate() {
		A.ABuilder resultBuilder = doEvaluate();
		
		final A result;
		if (resultBuilder == null) {
			result = null;
		} else {
			result = resultBuilder.build();
			objectValidator.validate(A.class, result);
		}
		
		return result;
	}

	protected abstract A.ABuilder doEvaluate();

	public static class TestDefault extends Test {
		@Override
		protected A.ABuilder doEvaluate() {
			A.ABuilder result = A.builder();
			return assignOutput(result);
		}
		
		protected A.ABuilder assignOutput(A.ABuilder result) {
			final String string = "Hello";
			result
				.addA((string == null ? Collections.<ReferenceWithMetaString>emptyList() : Collections.singletonList(ReferenceWithMetaString.builder().setValue(string).build())));
			
			return Optional.ofNullable(result)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
