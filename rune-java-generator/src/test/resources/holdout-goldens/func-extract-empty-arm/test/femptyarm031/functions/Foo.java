package test.femptyarm031.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(Foo.FooDefault.class)
public abstract class Foo implements RosettaFunction {

	/**
	* @return result 
	*/
	public Integer evaluate() {
		Integer result = doEvaluate();
		
		return result;
	}

	protected abstract Integer doEvaluate();

	public static class FooDefault extends Foo {
		@Override
		protected Integer doEvaluate() {
			Integer result = null;
			return assignOutput(result);
		}
		
		protected Integer assignOutput(Integer result) {
			result = MapperS.of(42)
				.mapSingleToItem(item -> {
					if (greaterThan(item, MapperS.of(0), CardinalityOperator.All).getOrDefault(false)) {
						return MapperS.<Integer>ofNull();
					}
					return item;
				}).get();
			
			return result;
		}
	}
}
