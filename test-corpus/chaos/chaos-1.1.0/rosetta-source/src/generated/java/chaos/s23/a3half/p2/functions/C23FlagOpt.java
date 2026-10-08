package chaos.s23.a3half.p2.functions;

import chaos.s23.a3half.p1.C23Box;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;


@ImplementedBy(C23FlagOpt.C23FlagOptDefault.class)
public abstract class C23FlagOpt implements RosettaFunction {

	/**
	* @param flag 
	* @param box 
	* @return r 
	*/
	public String evaluate(Boolean flag, C23Box box) {
		String r = doEvaluate(flag, box);
		
		return r;
	}

	protected abstract String doEvaluate(Boolean flag, C23Box box);

	public static class C23FlagOptDefault extends C23FlagOpt {
		@Override
		protected String doEvaluate(Boolean flag, C23Box box) {
			String r = null;
			return assignOutput(r, flag, box);
		}
		
		protected String assignOutput(String r, Boolean flag, C23Box box) {
			if ((flag == null ? false : flag)) {
				r = MapperS.of(box).<String>map("getLid", c23Box -> c23Box.getLid()).get();
			} else if (MapperS.of(box).<Boolean>map("getLive", c23Box -> c23Box.getLive()).getOrDefault(false)) {
				r = "live";
			} else {
				r = "none";
			}
			
			return r;
		}
	}
}
