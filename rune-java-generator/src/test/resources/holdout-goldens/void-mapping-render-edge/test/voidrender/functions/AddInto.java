package test.voidrender.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@ImplementedBy(AddInto.AddIntoDefault.class)
public abstract class AddInto implements RosettaFunction {

	/**
	* @param t 
	* @return rs 
	*/
	public List<Void> evaluate(Void t) {
		List<Void> rs = doEvaluate(t);
		
		return rs;
	}

	protected abstract List<Void> doEvaluate(Void t);

	public static class AddIntoDefault extends AddInto {
		@Override
		protected List<Void> doEvaluate(Void t) {
			List<Void> rs = new ArrayList<>();
			return assignOutput(rs, t);
		}
		
		protected List<Void> assignOutput(List<Void> rs, Void t) {
			rs.addAll(Collections.<Void>emptyList());
			
			return rs;
		}
	}
}
