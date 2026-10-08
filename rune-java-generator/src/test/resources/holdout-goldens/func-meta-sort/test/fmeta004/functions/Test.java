package test.fmeta004.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.metafields.FieldWithMetaString;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@ImplementedBy(Test.TestDefault.class)
public abstract class Test implements RosettaFunction {

	/**
	* @param myInputs 
	* @return result 
	*/
	public List<String> evaluate(List<? extends FieldWithMetaString> myInputs) {
		List<String> result = doEvaluate(myInputs);
		
		return result;
	}

	protected abstract List<String> doEvaluate(List<? extends FieldWithMetaString> myInputs);

	public static class TestDefault extends Test {
		@Override
		protected List<String> doEvaluate(List<? extends FieldWithMetaString> myInputs) {
			if (myInputs == null) {
				myInputs = Collections.emptyList();
			}
			List<String> result = new ArrayList<>();
			return assignOutput(result, myInputs);
		}
		
		protected List<String> assignOutput(List<String> result, List<? extends FieldWithMetaString> myInputs) {
			result = MapperC.<FieldWithMetaString>of(myInputs)
				.sort(lambdaParam -> lambdaParam.<String>map("Type coercion", fieldWithMetaString -> fieldWithMetaString == null ? null : fieldWithMetaString.getValue())).<String>map("Type coercion", _fieldWithMetaString -> _fieldWithMetaString.getValue()).getMulti();
			
			return result;
		}
	}
}
