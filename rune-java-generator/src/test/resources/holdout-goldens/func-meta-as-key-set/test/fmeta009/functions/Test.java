package test.fmeta009.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import java.util.Optional;
import javax.inject.Inject;
import test.fmeta009.Bar;
import test.fmeta009.Foo;
import test.fmeta009.metafields.ReferenceWithMetaFoo;


@ImplementedBy(Test.TestDefault.class)
public abstract class Test implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param myInput 
	* @return result 
	*/
	public Bar evaluate(ReferenceWithMetaFoo myInput) {
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

	protected abstract Bar.BarBuilder doEvaluate(ReferenceWithMetaFoo myInput);

	public static class TestDefault extends Test {
		@Override
		protected Bar.BarBuilder doEvaluate(ReferenceWithMetaFoo myInput) {
			Bar.BarBuilder result = Bar.builder();
			return assignOutput(result, myInput);
		}
		
		protected Bar.BarBuilder assignOutput(Bar.BarBuilder result, ReferenceWithMetaFoo myInput) {
			final Foo resultB = myInput == null ? null : myInput.getValue();
			result
				.setB(ReferenceWithMetaFoo.builder()
					.setGlobalReference(Optional.ofNullable(resultB)
						.map(r -> r.getMeta())
						.map(m -> m.getGlobalKey())
						.orElse(null))
					.setExternalReference(Optional.ofNullable(resultB)
						.map(r -> r.getMeta())
						.map(m -> m.getExternalKey())
						.orElse(null))
					.build()
				);
			
			return Optional.ofNullable(result)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
