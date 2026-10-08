package test.fmeta003.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.validation.ChoiceRuleValidationMethod;
import java.util.Arrays;
import test.fmeta003.Foo;
import test.fmeta003.metafields.FieldWithMetaFoo;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(Test.TestDefault.class)
public abstract class Test implements RosettaFunction {

	/**
	* @param foo 
	* @return result 
	*/
	public Boolean evaluate(FieldWithMetaFoo foo) {
		Boolean result = doEvaluate(foo);
		
		return result;
	}

	protected abstract Boolean doEvaluate(FieldWithMetaFoo foo);

	public static class TestDefault extends Test {
		@Override
		protected Boolean doEvaluate(FieldWithMetaFoo foo) {
			Boolean result = null;
			return assignOutput(result, foo);
		}
		
		protected Boolean assignOutput(Boolean result, FieldWithMetaFoo foo) {
			result = choice((foo == null ? MapperS.<Foo>ofNull() : MapperS.of(foo.getValue())), Arrays.asList("b", "c"), ChoiceRuleValidationMethod.REQUIRED).get();
			
			return result;
		}
	}
}
