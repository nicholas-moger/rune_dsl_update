package test.voidrender.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@ImplementedBy(IntoList.IntoListDefault.class)
public abstract class IntoList implements RosettaFunction {

	/**
	* @param ts 
	* @return rs 
	*/
	public List<Void> evaluate(List<Void> ts) {
		List<Void> rs = doEvaluate(ts);
		
		return rs;
	}

	protected abstract List<Void> doEvaluate(List<Void> ts);

	public static class IntoListDefault extends IntoList {
		@Override
		protected List<Void> doEvaluate(List<Void> ts) {
			if (ts == null) {
				ts = Collections.emptyList();
			}
			List<Void> rs = new ArrayList<>();
			return assignOutput(rs, ts);
		}
		
		protected List<Void> assignOutput(List<Void> rs, List<Void> ts) {
			rs = Collections.<Void>emptyList();
			
			return rs;
		}
	}
}
