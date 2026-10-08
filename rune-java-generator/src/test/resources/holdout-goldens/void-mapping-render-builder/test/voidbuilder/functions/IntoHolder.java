package test.voidbuilder.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import java.util.Optional;
import javax.inject.Inject;
import test.voidbuilder.Holder;


@ImplementedBy(IntoHolder.IntoHolderDefault.class)
public abstract class IntoHolder implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param t 
	* @return h 
	*/
	public Holder evaluate(Void t) {
		Holder.HolderBuilder hBuilder = doEvaluate(t);
		
		final Holder h;
		if (hBuilder == null) {
			h = null;
		} else {
			h = hBuilder.build();
			objectValidator.validate(Holder.class, h);
		}
		
		return h;
	}

	protected abstract Holder.HolderBuilder doEvaluate(Void t);

	public static class IntoHolderDefault extends IntoHolder {
		@Override
		protected Holder.HolderBuilder doEvaluate(Void t) {
			Holder.HolderBuilder h = Holder.builder();
			return assignOutput(h, t);
		}
		
		protected Holder.HolderBuilder assignOutput(Holder.HolderBuilder h, Void t) {
			h = null;
			
			return Optional.ofNullable(h)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
