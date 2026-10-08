package test.frec029.functions;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.records.Date;


@ImplementedBy(Bar.BarDefault.class)
public abstract class Bar implements RosettaFunction {

	/**
	* @param day 
	* @return result 
	*/
	public Date evaluate(Integer day) {
		Date result = doEvaluate(day);
		
		return result;
	}

	protected abstract Date doEvaluate(Integer day);

	public static class BarDefault extends Bar {
		@Override
		protected Date doEvaluate(Integer day) {
			Date result = null;
			return assignOutput(result, day);
		}
		
		protected Date assignOutput(Date result, Integer day) {
			if (day != null) {
				result = Date.of(2024, 2, day);
			} else {
				result = null;
			}
			
			return result;
		}
	}
}
