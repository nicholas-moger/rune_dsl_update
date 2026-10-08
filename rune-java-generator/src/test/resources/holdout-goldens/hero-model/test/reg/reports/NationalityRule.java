package test.reg.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import test.reg.CountryEnum;
import test.reg.Person;


@ImplementedBy(NationalityRule.NationalityRuleDefault.class)
public abstract class NationalityRule implements ReportFunction<Person, CountryEnum> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public CountryEnum evaluate(Person input) {
		CountryEnum output = doEvaluate(input);
		
		return output;
	}

	protected abstract CountryEnum doEvaluate(Person input);

	public static class NationalityRuleDefault extends NationalityRule {
		@Override
		protected CountryEnum doEvaluate(Person input) {
			CountryEnum output = null;
			return assignOutput(output, input);
		}
		
		protected CountryEnum assignOutput(CountryEnum output, Person input) {
			output = MapperS.of(input)
				.mapSingleToItem(item -> item.<CountryEnum>map("getNationality", person -> person.getNationality())).get();
			
			return output;
		}
	}
}
