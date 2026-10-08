package test.voidhoist.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@ImplementedBy(AddConditional.AddConditionalDefault.class)
public abstract class AddConditional implements RosettaFunction {

	/**
	* @param flag 
	* @param t 
	* @param u 
	* @return rs 
	*/
	public List<Void> evaluate(Boolean flag, Void t, Void u) {
		List<Void> rs = doEvaluate(flag, t, u);
		
		return rs;
	}

	protected abstract List<Void> doEvaluate(Boolean flag, Void t, Void u);

	public static class AddConditionalDefault extends AddConditional {
		@Override
		protected List<Void> doEvaluate(Boolean flag, Void t, Void u) {
			List<Void> rs = new ArrayList<>();
			return assignOutput(rs, flag, t, u);
		}
		
		protected List<Void> assignOutput(List<Void> rs, Boolean flag, Void t, Void u) {
			rs.addAll(Collections.<Void>emptyList());
			
			return rs;
		}
	}
}
