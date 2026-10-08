package test.boolnavlogical.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import test.boolnavlogical.Foo;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(FuncFooAnd2.FuncFooAnd2Default.class)
public abstract class FuncFooAnd2 implements RosettaFunction {

	/**
	* @param foo 
	* @return result 
	*/
	public Boolean evaluate(Foo foo) {
		Boolean result = doEvaluate(foo);
		
		return result;
	}

	protected abstract Boolean doEvaluate(Foo foo);

	public static class FuncFooAnd2Default extends FuncFooAnd2 {
		@Override
		protected Boolean doEvaluate(Foo foo) {
			Boolean result = null;
			return assignOutput(result, foo);
		}
		
		protected Boolean assignOutput(Boolean result, Foo foo) {
			result = areEqual(MapperS.of(foo).<BigDecimal>map("getAttrNumber", _foo -> _foo.getAttrNumber()), MapperS.of(BigDecimal.valueOf(5)), CardinalityOperator.All).andNullSafe(ComparisonResult.ofNullSafe(MapperS.of(foo).<Boolean>map("getAttrBoolean", _foo -> _foo.getAttrBoolean()))).get();
			
			return result;
		}
	}
}
