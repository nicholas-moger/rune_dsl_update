package com.rosetta.test.model.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;


@ImplementedBy(F3.F3Default.class)
public abstract class F3 implements RosettaFunction {
	
	// RosettaFunction dependencies
	//
	@Inject protected IsAnswerToTheUniverse isAnswerToTheUniverse;

	/**
	* @param list 
	* @return res 
	*/
	public List<Integer> evaluate(List<Integer> list) {
		List<Integer> res = doEvaluate(list);
		
		return res;
	}

	protected abstract List<Integer> doEvaluate(List<Integer> list);

	public static class F3Default extends F3 {
		@Override
		protected List<Integer> doEvaluate(List<Integer> list) {
			if (list == null) {
				list = Collections.emptyList();
			}
			List<Integer> res = new ArrayList<>();
			return assignOutput(res, list);
		}
		
		protected List<Integer> assignOutput(List<Integer> res, List<Integer> list) {
			res.addAll(MapperC.<Integer>of(list)
				.filterItemNullSafe(item -> isAnswerToTheUniverse.evaluate(item.get())).getMulti());
			
			return res;
		}
	}
}
