package test.expressions.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.metafields.FieldWithMetaString;
import com.rosetta.model.metafields.MetaFields;


@ImplementedBy(TestWithMeta.TestWithMetaDefault.class)
public abstract class TestWithMeta implements RosettaFunction {

	/**
	* @param value 
	* @return result 
	*/
	public String evaluate(String value) {
		String result = doEvaluate(value);
		
		return result;
	}

	protected abstract String doEvaluate(String value);

	public static class TestWithMetaDefault extends TestWithMeta {
		@Override
		protected String doEvaluate(String value) {
			String result = null;
			return assignOutput(result, value);
		}
		
		protected String assignOutput(String result, String value) {
			final String withMetaArgument = value;
			final FieldWithMetaString fieldWithMetaString = FieldWithMetaString.builder().setValue(withMetaArgument).setMeta(MetaFields.builder().setScheme("http://example.com"));
			if (fieldWithMetaString == null) {
				result = null;
			} else {
				result = fieldWithMetaString.getValue();
			}
			
			return result;
		}
	}
}
