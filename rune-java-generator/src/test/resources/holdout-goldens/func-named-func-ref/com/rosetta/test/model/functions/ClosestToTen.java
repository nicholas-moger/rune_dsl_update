package com.rosetta.test.model.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.expression.MapperMaths;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(ClosestToTen.ClosestToTenDefault.class)
public abstract class ClosestToTen implements RosettaFunction {

	/**
	* @param a 
	* @param b 
	* @return result 
	*/
	public Integer evaluate(Integer a, Integer b) {
		Integer result = doEvaluate(a, b);
		
		return result;
	}

	protected abstract Integer doEvaluate(Integer a, Integer b);

	public static class ClosestToTenDefault extends ClosestToTen {
		@Override
		protected Integer doEvaluate(Integer a, Integer b) {
			Integer result = null;
			return assignOutput(result, a, b);
		}
		
		protected Integer assignOutput(Integer result, Integer a, Integer b) {
			if (lessThan(MapperS.of(a), MapperS.of(10), CardinalityOperator.All).getOrDefault(false)) {
				if (lessThan(MapperS.of(b), MapperS.of(10), CardinalityOperator.All).getOrDefault(false)) {
					if (greaterThan(MapperS.of(a), MapperS.of(b), CardinalityOperator.All).getOrDefault(false)) {
						result = a;
					} else {
						result = b;
					}
				} else if (lessThan(MapperMaths.<Integer, Integer, Integer>subtract(MapperS.of(10), MapperS.of(a)), MapperMaths.<Integer, Integer, Integer>subtract(MapperS.of(b), MapperS.of(10)), CardinalityOperator.All).getOrDefault(false)) {
					result = a;
				} else {
					result = b;
				}
			} else if (lessThan(MapperS.of(b), MapperS.of(10), CardinalityOperator.All).getOrDefault(false)) {
				if (lessThan(MapperMaths.<Integer, Integer, Integer>subtract(MapperS.of(a), MapperS.of(10)), MapperMaths.<Integer, Integer, Integer>subtract(MapperS.of(10), MapperS.of(b)), CardinalityOperator.All).getOrDefault(false)) {
					result = a;
				} else {
					result = b;
				}
			} else if (lessThan(MapperS.of(a), MapperS.of(b), CardinalityOperator.All).getOrDefault(false)) {
				result = a;
			} else {
				result = b;
			}
			
			return result;
		}
	}
}
