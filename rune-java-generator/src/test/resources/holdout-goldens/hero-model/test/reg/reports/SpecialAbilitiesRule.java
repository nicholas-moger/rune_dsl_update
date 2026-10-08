package test.reg.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import test.reg.Person;


@ImplementedBy(SpecialAbilitiesRule.SpecialAbilitiesRuleDefault.class)
public abstract class SpecialAbilitiesRule implements ReportFunction<Person, Boolean> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public Boolean evaluate(Person input) {
		Boolean output = doEvaluate(input);
		
		return output;
	}

	protected abstract Boolean doEvaluate(Person input);

	public static class SpecialAbilitiesRuleDefault extends SpecialAbilitiesRule {
		@Override
		protected Boolean doEvaluate(Person input) {
			Boolean output = null;
			return assignOutput(output, input);
		}
		
		protected Boolean assignOutput(Boolean output, Person input) {
			output = MapperS.of(input)
				.mapSingleToItem(item -> item.<Boolean>map("getHasSpecialAbilities", person -> person.getHasSpecialAbilities())).get();
			
			return output;
		}
	}
}
