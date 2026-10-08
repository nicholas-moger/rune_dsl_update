package test.fmeta005.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.metafields.FieldWithMetaInteger;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


@ImplementedBy(Test.TestDefault.class)
public abstract class Test implements RosettaFunction {

	/**
	* @param n 
	* @return result 
	*/
	public Integer evaluate(List<? extends FieldWithMetaInteger> n) {
		Integer result = doEvaluate(n);
		
		return result;
	}

	protected abstract Integer doEvaluate(List<? extends FieldWithMetaInteger> n);

	public static class TestDefault extends Test {
		@Override
		protected Integer doEvaluate(List<? extends FieldWithMetaInteger> n) {
			if (n == null) {
				n = Collections.emptyList();
			}
			Integer result = null;
			return assignOutput(result, n);
		}
		
		protected Integer assignOutput(Integer result, List<? extends FieldWithMetaInteger> n) {
			result = MapperC.<Integer>of(n.stream()
				.<Integer>map(fieldWithMetaInteger -> fieldWithMetaInteger.getValue())
				.collect(Collectors.toList())
			)
				.sumInteger().get();
			
			return result;
		}
	}
}
