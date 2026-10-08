package test.reg.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import test.reg.Person;


@ImplementedBy(HeroNameRule.HeroNameRuleDefault.class)
public abstract class HeroNameRule implements ReportFunction<Person, String> {

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

	public static class HeroNameRuleDefault extends HeroNameRule {
		@Override
		protected String doEvaluate(Person input) {
			String output = null;
			return assignOutput(output, input);
		}
		
		protected String assignOutput(String output, Person input) {
			output = MapperS.of(input)
				.mapSingleToItem(item -> item.<String>map("getName", person -> person.getName())).get();
			
			return output;
		}
	}
}
