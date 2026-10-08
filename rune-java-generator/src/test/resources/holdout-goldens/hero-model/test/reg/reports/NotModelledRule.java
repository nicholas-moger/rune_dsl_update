package test.reg.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.reports.ReportFunction;
import test.reg.Person;


@ImplementedBy(NotModelledRule.NotModelledRuleDefault.class)
public abstract class NotModelledRule implements ReportFunction<Person, String> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public String evaluate(Person input) {
		String output = doEvaluate(input);
		
		return output;
	}

	protected abstract String doEvaluate(Person input);

	public static class NotModelledRuleDefault extends NotModelledRule {
		@Override
		protected String doEvaluate(Person input) {
			String output = null;
			return assignOutput(output, input);
		}
		
		protected String assignOutput(String output, Person input) {
			output = "Not modelled";
			
			return output;
		}
	}
}
