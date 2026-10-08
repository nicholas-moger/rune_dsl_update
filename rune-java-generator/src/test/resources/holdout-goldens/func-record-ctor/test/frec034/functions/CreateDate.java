package test.frec034.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.records.Date;


@ImplementedBy(CreateDate.CreateDateDefault.class)
public abstract class CreateDate implements RosettaFunction {

	/**
	* @return result 
	*/
	public Date evaluate() {
		Date result = doEvaluate();
		
		return result;
	}

	protected abstract Date doEvaluate();

	public static class CreateDateDefault extends CreateDate {
		@Override
		protected Date doEvaluate() {
			Date result = null;
			return assignOutput(result);
		}
		
		protected Date assignOutput(Date result) {
			result = Date.of(1998, 11, 4);
			
			return result;
		}
	}
}
