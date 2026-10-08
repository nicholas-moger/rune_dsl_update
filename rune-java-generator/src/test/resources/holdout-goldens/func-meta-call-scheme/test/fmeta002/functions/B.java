package test.fmeta002.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.metafields.FieldWithMetaString;
import javax.inject.Inject;


@ImplementedBy(B.BDefault.class)
public abstract class B implements RosettaFunction {
	
	// RosettaFunction dependencies
	//
	@Inject protected A a;

	/**
	* @param myInput 
	* @return result 
	*/
	public String evaluate(FieldWithMetaString myInput) {
		String result = doEvaluate(myInput);
		
		return result;
	}

	protected abstract String doEvaluate(FieldWithMetaString myInput);

	public static class BDefault extends B {
		@Override
		protected String doEvaluate(FieldWithMetaString myInput) {
			String result = null;
			return assignOutput(result, myInput);
		}
		
		protected String assignOutput(String result, FieldWithMetaString myInput) {
			result = MapperS.of(a.evaluate(myInput)).map("getMeta", _a->_a.getMeta()).map("getScheme", _a->_a.getScheme()).get();
			
			return result;
		}
	}
}
