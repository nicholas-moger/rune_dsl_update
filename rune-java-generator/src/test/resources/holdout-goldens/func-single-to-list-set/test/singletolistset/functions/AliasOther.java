package test.singletolistset.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import java.math.BigDecimal;
import java.util.Optional;
import javax.inject.Inject;
import test.singletolistset.Baz;
import test.singletolistset.Foo;
import test.singletolistset.NumberList;


@ImplementedBy(AliasOther.AliasOtherDefault.class)
public abstract class AliasOther implements RosettaFunction {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param foo 
	* @return result 
	*/
	public NumberList evaluate(Foo foo) {
		NumberList.NumberListBuilder resultBuilder = doEvaluate(foo);
		
		final NumberList result;
		if (resultBuilder == null) {
			result = null;
		} else {
			result = resultBuilder.build();
			objectValidator.validate(NumberList.class, result);
		}
		
		return result;
	}

	protected abstract NumberList.NumberListBuilder doEvaluate(Foo foo);

	public static class AliasOtherDefault extends AliasOther {
		@Override
		protected NumberList.NumberListBuilder doEvaluate(Foo foo) {
			NumberList.NumberListBuilder result = NumberList.builder();
			return assignOutput(result, foo);
		}
		
		protected NumberList.NumberListBuilder assignOutput(NumberList.NumberListBuilder result, Foo foo) {
			result
				.setNumbers(MapperS.of(foo).<Baz>map("getBaz", _foo -> _foo.getBaz()).<BigDecimal>map("getOther", baz -> baz.getOther()).getMulti());
			
			return Optional.ofNullable(result)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
