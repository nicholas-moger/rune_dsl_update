package com.rosetta.test.model.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(IsAnswerToTheUniverse.IsAnswerToTheUniverseDefault.class)
public abstract class IsAnswerToTheUniverse implements RosettaFunction {

	/**
	* @param a 
	* @return result 
	*/
	public Boolean evaluate(Integer a) {
		Boolean result = doEvaluate(a);
		
		return result;
	}

	protected abstract Boolean doEvaluate(Integer a);

	public static class IsAnswerToTheUniverseDefault extends IsAnswerToTheUniverse {
		@Override
		protected Boolean doEvaluate(Integer a) {
			Boolean result = null;
			return assignOutput(result, a);
		}
		
		protected Boolean assignOutput(Boolean result, Integer a) {
			result = areEqual(MapperS.of(a), MapperS.of(42), CardinalityOperator.All).get();
			
			return result;
		}
	}
}
