package holdout.voidemptyelse.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@ImplementedBy(AddEmptyElse.AddEmptyElseDefault.class)
public abstract class AddEmptyElse implements RosettaFunction {

	/**
	* @param flag 
	* @param t 
	* @return rs 
	*/
	public List<Void> evaluate(Boolean flag, Void t) {
		List<Void> rs = doEvaluate(flag, t);
		
		return rs;
	}

	protected abstract List<Void> doEvaluate(Boolean flag, Void t);

	public static class AddEmptyElseDefault extends AddEmptyElse {
		@Override
		protected List<Void> doEvaluate(Boolean flag, Void t) {
			List<Void> rs = new ArrayList<>();
			return assignOutput(rs, flag, t);
		}
		
		protected List<Void> assignOutput(List<Void> rs, Boolean flag, Void t) {
			rs.addAll(Collections.<Void>emptyList());
			
			return rs;
		}
	}
}
