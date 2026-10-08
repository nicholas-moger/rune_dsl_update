package test.fsingle049.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.mapper.MapperS;


@ImplementedBy(Boolean.BooleanDefault.class)
public abstract class Boolean implements RosettaFunction {

	/**
	* @return _Boolean 
	*/
	public java.lang.Boolean evaluate() {
		java.lang.Boolean _Boolean = doEvaluate();
		
		return _Boolean;
	}

	protected abstract java.lang.Boolean doEvaluate();

	public static class BooleanDefault extends Boolean {
		@Override
		protected java.lang.Boolean doEvaluate() {
			java.lang.Boolean _Boolean = null;
			return assignOutput(_Boolean);
		}
		
		protected java.lang.Boolean assignOutput(java.lang.Boolean _Boolean) {
			_Boolean = MapperS.of(true)
				.mapSingleToItem(item -> MapperS.of(false)).get();
			
			return _Boolean;
		}
	}
}
