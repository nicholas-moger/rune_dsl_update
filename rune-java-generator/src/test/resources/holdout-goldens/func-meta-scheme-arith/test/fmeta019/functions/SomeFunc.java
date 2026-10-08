package test.fmeta019.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.MapperMaths;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.metafields.FieldWithMetaString;


@ImplementedBy(SomeFunc.SomeFuncDefault.class)
public abstract class SomeFunc implements RosettaFunction {

	/**
	* @param myInput 
	* @return myResult 
	*/
	public String evaluate(FieldWithMetaString myInput) {
		String myResult = doEvaluate(myInput);
		
		return myResult;
	}

	protected abstract String doEvaluate(FieldWithMetaString myInput);

	public static class SomeFuncDefault extends SomeFunc {
		@Override
		protected String doEvaluate(FieldWithMetaString myInput) {
			String myResult = null;
			return assignOutput(myResult, myInput);
		}
		
		protected String assignOutput(String myResult, FieldWithMetaString myInput) {
			myResult = MapperMaths.<String, String, String>add((myInput == null ? MapperS.<String>ofNull() : MapperS.of(myInput.getValue())), MapperS.of(myInput).map("getMeta", a->a.getMeta()).map("getScheme", a->a.getScheme())).get();
			
			return myResult;
		}
	}
}
