package test.reg.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import java.util.Optional;
import javax.inject.Inject;
import test.reg.Person;


@ImplementedBy(HasSuperPowersRule.HasSuperPowersRuleDefault.class)
public abstract class HasSuperPowersRule implements ReportFunction<Person, Person> {
	
	@Inject protected ModelObjectValidator objectValidator;

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public Person evaluate(Person input) {
		Person.PersonBuilder outputBuilder = doEvaluate(input);
		
		final Person output;
		if (outputBuilder == null) {
			output = null;
		} else {
			output = outputBuilder.build();
			objectValidator.validate(Person.class, output);
		}
		
		return output;
	}

	protected abstract Person.PersonBuilder doEvaluate(Person input);

	public static class HasSuperPowersRuleDefault extends HasSuperPowersRule {
		@Override
		protected Person.PersonBuilder doEvaluate(Person input) {
			Person.PersonBuilder output = Person.builder();
			return assignOutput(output, input);
		}
		
		protected Person.PersonBuilder assignOutput(Person.PersonBuilder output, Person input) {
			output = toBuilder(MapperS.of(input)
				.filterSingleNullSafe(item -> item.<Boolean>map("getHasSpecialAbilities", person -> person.getHasSpecialAbilities()).get()).get());
			
			return Optional.ofNullable(output)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
