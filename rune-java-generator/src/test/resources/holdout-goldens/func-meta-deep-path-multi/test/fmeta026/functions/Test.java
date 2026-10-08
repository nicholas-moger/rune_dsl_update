package test.fmeta026.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.metafields.FieldWithMetaInteger;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import test.fmeta026.A;
import test.fmeta026.util.ADeepPathUtil;


@ImplementedBy(Test.TestDefault.class)
public abstract class Test implements RosettaFunction {
	
	// RosettaFunction dependencies
	//
	@Inject protected ADeepPathUtil aDeepPathUtil;

	/**
	* @param a 
	* @return result 
	*/
	public List<Integer> evaluate(A a) {
		List<Integer> result = doEvaluate(a);
		
		return result;
	}

	protected abstract List<Integer> doEvaluate(A a);

	public static class TestDefault extends Test {
		@Override
		protected List<Integer> doEvaluate(A a) {
			List<Integer> result = new ArrayList<>();
			return assignOutput(result, a);
		}
		
		protected List<Integer> assignOutput(List<Integer> result, A a) {
			result = MapperS.of(a).<FieldWithMetaInteger>mapC("chooseProp", _a -> aDeepPathUtil.chooseProp(_a)).<Integer>map("Type coercion", fieldWithMetaInteger -> fieldWithMetaInteger.getValue()).getMulti();
			
			return result;
		}
	}
}
