package com.rosetta.test.model.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;


@ImplementedBy(F1.F1Default.class)
public abstract class F1 implements RosettaFunction {
	
	// RosettaFunction dependencies
	//
	@Inject protected Incr incr;

	/**
	* @param list 
	* @return res 
	*/
	public List<Integer> evaluate(List<Integer> list) {
		List<Integer> res = doEvaluate(list);
		
		return res;
	}

	protected abstract List<Integer> doEvaluate(List<Integer> list);

	public static class F1Default extends F1 {
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
				.mapItem(item -> MapperS.of(incr.evaluate(item.get()))).getMulti());
			
			return res;
		}
	}
}
