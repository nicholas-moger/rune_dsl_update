package com.rosetta.test.model.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;


@ImplementedBy(F4.F4Default.class)
public abstract class F4 implements RosettaFunction {
	
	// RosettaFunction dependencies
	//
	@Inject protected ClosestToTen closestToTen;

	/**
	* @param list 
	* @return res 
	*/
	public Integer evaluate(List<Integer> list) {
		Integer res = doEvaluate(list);
		
		return res;
	}

	protected abstract Integer doEvaluate(List<Integer> list);

	public static class F4Default extends F4 {
		@Override
		protected Integer doEvaluate(List<Integer> list) {
			if (list == null) {
				list = Collections.emptyList();
			}
			Integer res = null;
			return assignOutput(res, list);
		}
		
		protected Integer assignOutput(Integer res, List<Integer> list) {
			res = MapperC.<Integer>of(list)
				.<Integer>reduce((acc, v) -> MapperS.of(closestToTen.evaluate(acc.get(), v.get()))).get();
			
			return res;
		}
	}
}
