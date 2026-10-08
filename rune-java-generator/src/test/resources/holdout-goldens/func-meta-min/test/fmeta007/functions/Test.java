package test.fmeta007.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.metafields.FieldWithMetaString;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


@ImplementedBy(Test.TestDefault.class)
public abstract class Test implements RosettaFunction {

	/**
	* @param myInputs 
	* @return result 
	*/
	public String evaluate(List<? extends FieldWithMetaString> myInputs) {
		String result = doEvaluate(myInputs);
		
		return result;
	}

	protected abstract String doEvaluate(List<? extends FieldWithMetaString> myInputs);

	public static class TestDefault extends Test {
		@Override
		protected String doEvaluate(List<? extends FieldWithMetaString> myInputs) {
			if (myInputs == null) {
				myInputs = Collections.emptyList();
			}
			String result = null;
			return assignOutput(result, myInputs);
		}
		
		protected String assignOutput(String result, List<? extends FieldWithMetaString> myInputs) {
			result = MapperC.<String>of(myInputs.stream()
				.<String>map(fieldWithMetaString -> fieldWithMetaString.getValue())
				.collect(Collectors.toList())
			)
				.min().get();
			
			return result;
		}
	}
}
