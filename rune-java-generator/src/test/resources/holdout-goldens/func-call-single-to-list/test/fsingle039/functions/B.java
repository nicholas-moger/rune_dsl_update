package test.fsingle039.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;


@ImplementedBy(B.BDefault.class)
public abstract class B implements RosettaFunction {
	
	// RosettaFunction dependencies
	//
	@Inject protected A a;

	/**
	* @return result 
	*/
	public List<Integer> evaluate() {
		List<Integer> result = doEvaluate();
		
		return result;
	}

	protected abstract List<Integer> doEvaluate();

	public static class BDefault extends B {
		@Override
		protected List<Integer> doEvaluate() {
			List<Integer> result = new ArrayList<>();
			return assignOutput(result);
		}
		
		protected List<Integer> assignOutput(List<Integer> result) {
			result.addAll(a.evaluate(Collections.<Integer>emptyList()));
			
			return result;
		}
	}
}
