package test.fomit057.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.MapperMaths;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@ImplementedBy(F1.F1Default.class)
public abstract class F1 implements RosettaFunction {

	/**
	* @param a 
	* @return result 
	*/
	public List<Integer> evaluate(List<Integer> a) {
		List<Integer> result = doEvaluate(a);
		
		return result;
	}

	protected abstract List<Integer> doEvaluate(List<Integer> a);

	public static class F1Default extends F1 {
		@Override
		protected List<Integer> doEvaluate(List<Integer> a) {
			if (a == null) {
				a = Collections.emptyList();
			}
			List<Integer> result = new ArrayList<>();
			return assignOutput(result, a);
		}
		
		protected List<Integer> assignOutput(List<Integer> result, List<Integer> a) {
			result.addAll(MapperC.<Integer>of(a)
				.mapItem(item -> MapperMaths.<Integer, Integer, Integer>multiply(item, MapperS.of(2))).getMulti());
			
			return result;
		}
	}
}
