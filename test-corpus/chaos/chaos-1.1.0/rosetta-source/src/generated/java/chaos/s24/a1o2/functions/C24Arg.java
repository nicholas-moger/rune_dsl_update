package chaos.s24.a1o2.functions;

import chaos.s24.a1o2.C24Carrier;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import javax.inject.Inject;


@ImplementedBy(C24Arg.C24ArgDefault.class)
public abstract class C24Arg implements RosettaFunction {
	
	// RosettaFunction dependencies
	//
	@Inject protected C24Pass c24Pass;

	/**
	* @param c 
	* @param d 
	* @param k 
	* @return r 
	*/
	public Void evaluate(C24Carrier c, C24Carrier d, String k) {
		Void r = doEvaluate(c, d, k);
		
		return r;
	}

	protected abstract Void doEvaluate(C24Carrier c, C24Carrier d, String k);

	protected abstract MapperS<Void> viaCall(C24Carrier c, C24Carrier d, String k);

	protected abstract MapperS<Void> listed(C24Carrier c, C24Carrier d, String k);

	protected abstract MapperS<Void> defaulted(C24Carrier c, C24Carrier d, String k);

	public static class C24ArgDefault extends C24Arg {
		@Override
		protected Void doEvaluate(C24Carrier c, C24Carrier d, String k) {
			Void r = null;
			return assignOutput(r, c, d, k);
		}
		
		protected Void assignOutput(Void r, C24Carrier c, C24Carrier d, String k) {
			r = null;
			
			return r;
		}
		
		@Override
		protected MapperS<Void> viaCall(C24Carrier c, C24Carrier d, String k) {
			return MapperS.<Void>ofNull();
		}
		
		@Override
		protected MapperS<Void> listed(C24Carrier c, C24Carrier d, String k) {
			return MapperS.<Void>ofNull();
		}
		
		@Override
		protected MapperS<Void> defaulted(C24Carrier c, C24Carrier d, String k) {
			return MapperS.<Void>ofNull();
		}
	}
}
