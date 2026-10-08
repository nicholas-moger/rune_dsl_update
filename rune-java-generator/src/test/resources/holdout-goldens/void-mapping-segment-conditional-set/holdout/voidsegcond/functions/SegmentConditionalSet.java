package holdout.voidsegcond.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import holdout.voidsegcond.Carrier;
import java.util.Optional;
import javax.inject.Inject;


@ImplementedBy(SegmentConditionalSet.SegmentConditionalSetDefault.class)
public abstract class SegmentConditionalSet implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param flag 
	* @param t 
	* @param u 
	* @return c 
	*/
	public Carrier evaluate(Boolean flag, Void t, Void u) {
		Carrier.CarrierBuilder cBuilder = doEvaluate(flag, t, u);
		
		final Carrier c;
		if (cBuilder == null) {
			c = null;
		} else {
			c = cBuilder.build();
			objectValidator.validate(Carrier.class, c);
		}
		
		return c;
	}

	protected abstract Carrier.CarrierBuilder doEvaluate(Boolean flag, Void t, Void u);

	public static class SegmentConditionalSetDefault extends SegmentConditionalSet {
		@Override
		protected Carrier.CarrierBuilder doEvaluate(Boolean flag, Void t, Void u) {
			Carrier.CarrierBuilder c = Carrier.builder();
			return assignOutput(c, flag, t, u);
		}
		
		protected Carrier.CarrierBuilder assignOutput(Carrier.CarrierBuilder c, Boolean flag, Void t, Void u) {
			c
				.setTok(null);
			
			return Optional.ofNullable(c)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
