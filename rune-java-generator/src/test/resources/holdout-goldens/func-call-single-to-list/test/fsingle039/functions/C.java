package test.fsingle039.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;


@ImplementedBy(C.CDefault.class)
public abstract class C implements RosettaFunction {
	
	// RosettaFunction dependencies
	//
	@Inject protected A a;

	/**
	* @param _a 
	* @return result 
	*/
	public List<Integer> evaluate(Integer _a) {
		List<Integer> result = doEvaluate(_a);
		
		return result;
	}

	protected abstract List<Integer> doEvaluate(Integer _a);

	public static class CDefault extends C {
		@Override
		protected List<Integer> doEvaluate(Integer _a) {
			List<Integer> result = new ArrayList<>();
			return assignOutput(result, _a);
		}
		
		protected List<Integer> assignOutput(List<Integer> result, Integer _a) {
			result.addAll(a.evaluate((_a == null ? Collections.<Integer>emptyList() : Collections.singletonList(_a))));
			
			return result;
		}
	}
}
