package test.fsetbasic144.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import java.util.Collections;
import java.util.Optional;
import javax.inject.Inject;
import test.fsetbasic144.Baz;


@ImplementedBy(FuncFoo.FuncFooDefault.class)
public abstract class FuncFoo implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param baz 
	* @param s Add single
	* @return updatedBaz 
	*/
	public Baz evaluate(Baz baz, String s) {
		Baz.BazBuilder updatedBazBuilder = doEvaluate(baz, s);
		
		final Baz updatedBaz;
		if (updatedBazBuilder == null) {
			updatedBaz = null;
		} else {
			updatedBaz = updatedBazBuilder.build();
			objectValidator.validate(Baz.class, updatedBaz);
		}
		
		return updatedBaz;
	}

	protected abstract Baz.BazBuilder doEvaluate(Baz baz, String s);

	public static class FuncFooDefault extends FuncFoo {
		@Override
		protected Baz.BazBuilder doEvaluate(Baz baz, String s) {
			Baz.BazBuilder updatedBaz = Baz.builder();
			return assignOutput(updatedBaz, baz, s);
		}
		
		protected Baz.BazBuilder assignOutput(Baz.BazBuilder updatedBaz, Baz baz, String s) {
			updatedBaz = toBuilder(baz, () -> Baz.builder());
			
			updatedBaz
				.setAttrList((s == null ? Collections.<String>emptyList() : Collections.singletonList(s)));
			
			return Optional.ofNullable(updatedBaz)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
