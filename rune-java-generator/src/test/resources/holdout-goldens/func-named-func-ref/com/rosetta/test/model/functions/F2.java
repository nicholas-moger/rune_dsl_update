package com.rosetta.test.model.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.IsLeapYear;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@ImplementedBy(F2.F2Default.class)
public abstract class F2 implements RosettaFunction {

	/**
	* @param list 
	* @return res 
	*/
	public List<Boolean> evaluate(List<Integer> list) {
		List<Boolean> res = doEvaluate(list);
		
		return res;
	}

	protected abstract List<Boolean> doEvaluate(List<Integer> list);

	public static class F2Default extends F2 {
		@Override
		protected List<Boolean> doEvaluate(List<Integer> list) {
			if (list == null) {
				list = Collections.emptyList();
			}
			List<Boolean> res = new ArrayList<>();
			return assignOutput(res, list);
		}
		
		protected List<Boolean> assignOutput(List<Boolean> res, List<Integer> list) {
			res.addAll(MapperC.<Integer>of(list)
				.mapItem(item -> {
					final Integer integer = item.get();
					return integer == null ? MapperS.<Boolean>ofNull() : MapperS.of(new IsLeapYear().execute(BigDecimal.valueOf(integer)));
				}).getMulti());
			
			return res;
		}
	}
}
