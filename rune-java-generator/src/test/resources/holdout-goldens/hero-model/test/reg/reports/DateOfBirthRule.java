package test.reg.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.records.Date;
import com.rosetta.model.lib.reports.ReportFunction;
import test.reg.Person;


@ImplementedBy(DateOfBirthRule.DateOfBirthRuleDefault.class)
public abstract class DateOfBirthRule implements ReportFunction<Person, Date> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public Date evaluate(Person input) {
		Date output = doEvaluate(input);
		
		return output;
	}

	protected abstract Date doEvaluate(Person input);

	public static class DateOfBirthRuleDefault extends DateOfBirthRule {
		@Override
		protected Date doEvaluate(Person input) {
			Date output = null;
			return assignOutput(output, input);
		}
		
		protected Date assignOutput(Date output, Person input) {
			output = MapperS.of(input)
				.mapSingleToItem(item -> item.<Date>map("getDateOfBirth", person -> person.getDateOfBirth())).get();
			
			return output;
		}
	}
}
