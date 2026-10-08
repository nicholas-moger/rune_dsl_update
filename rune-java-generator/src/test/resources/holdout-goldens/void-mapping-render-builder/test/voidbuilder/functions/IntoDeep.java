package test.voidbuilder.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import java.util.Optional;
import javax.inject.Inject;
import test.voidbuilder.Outer;


@ImplementedBy(IntoDeep.IntoDeepDefault.class)
public abstract class IntoDeep implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param t 
	* @return o 
	*/
	public Outer evaluate(Void t) {
		Outer.OuterBuilder oBuilder = doEvaluate(t);
		
		final Outer o;
		if (oBuilder == null) {
			o = null;
		} else {
			o = oBuilder.build();
			objectValidator.validate(Outer.class, o);
		}
		
		return o;
	}

	protected abstract Outer.OuterBuilder doEvaluate(Void t);

	public static class IntoDeepDefault extends IntoDeep {
		@Override
		protected Outer.OuterBuilder doEvaluate(Void t) {
			Outer.OuterBuilder o = Outer.builder();
			return assignOutput(o, t);
		}
		
		protected Outer.OuterBuilder assignOutput(Outer.OuterBuilder o, Void t) {
			o
				.setHolder(null);
			
			return Optional.ofNullable(o)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
