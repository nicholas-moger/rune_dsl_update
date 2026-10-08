package test.fsetcplx140.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import java.util.Collections;
import java.util.Optional;
import javax.inject.Inject;
import test.fsetcplx140.Bar;
import test.fsetcplx140.Foo;


@ImplementedBy(FuncFoo.FuncFooDefault.class)
public abstract class FuncFoo implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param bar 
	* @param newFoo Add single Foo
	* @return updatedBar 
	*/
	public Bar evaluate(Bar bar, Foo newFoo) {
		Bar.BarBuilder updatedBarBuilder = doEvaluate(bar, newFoo);
		
		final Bar updatedBar;
		if (updatedBarBuilder == null) {
			updatedBar = null;
		} else {
			updatedBar = updatedBarBuilder.build();
			objectValidator.validate(Bar.class, updatedBar);
		}
		
		return updatedBar;
	}

	protected abstract Bar.BarBuilder doEvaluate(Bar bar, Foo newFoo);

	public static class FuncFooDefault extends FuncFoo {
		@Override
		protected Bar.BarBuilder doEvaluate(Bar bar, Foo newFoo) {
			Bar.BarBuilder updatedBar = Bar.builder();
			return assignOutput(updatedBar, bar, newFoo);
		}
		
		protected Bar.BarBuilder assignOutput(Bar.BarBuilder updatedBar, Bar bar, Foo newFoo) {
			updatedBar = toBuilder(bar, () -> Bar.builder());
			
			updatedBar
				.setFoos((newFoo == null ? Collections.<Foo>emptyList() : Collections.singletonList(newFoo)));
			
			return Optional.ofNullable(updatedBar)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
