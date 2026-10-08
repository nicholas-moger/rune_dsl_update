package holdout.argcoercionbarelocal.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import holdout.argcoercionbarelocal.Holder;
import java.math.BigDecimal;
import javax.inject.Inject;


@ImplementedBy(CallWithIntNav.CallWithIntNavDefault.class)
public abstract class CallWithIntNav implements RosettaFunction {
	
	// RosettaFunction dependencies
	//
	@Inject protected Check check;

	/**
	* @param h 
	* @return ok 
	*/
	public Boolean evaluate(Holder h) {
		Boolean ok = doEvaluate(h);
		
		return ok;
	}

	protected abstract Boolean doEvaluate(Holder h);

	public static class CallWithIntNavDefault extends CallWithIntNav {
		@Override
		protected Boolean doEvaluate(Holder h) {
			Boolean ok = null;
			return assignOutput(ok, h);
		}
		
		protected Boolean assignOutput(Boolean ok, Holder h) {
			final Integer integer = MapperS.of(h).<Integer>map("getTally", holder -> holder.getTally()).get();
			ok = check.evaluate((integer == null ? null : BigDecimal.valueOf(integer)));
			
			return ok;
		}
	}
}
