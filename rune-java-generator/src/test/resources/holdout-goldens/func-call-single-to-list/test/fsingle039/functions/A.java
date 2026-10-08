package test.fsingle039.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@ImplementedBy(A.ADefault.class)
public abstract class A implements RosettaFunction {

	/**
	* @param a 
	* @return result 
	*/
	public List<Integer> evaluate(List<Integer> a) {
		List<Integer> result = doEvaluate(a);
		
		return result;
	}

	protected abstract List<Integer> doEvaluate(List<Integer> a);

	public static class ADefault extends A {
		@Override
		protected List<Integer> doEvaluate(List<Integer> a) {
			if (a == null) {
				a = Collections.emptyList();
			}
			List<Integer> result = new ArrayList<>();
			return assignOutput(result, a);
		}
		
		protected List<Integer> assignOutput(List<Integer> result, List<Integer> a) {
			result.addAll(a);
			
			return result;
		}
	}
}
