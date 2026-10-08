package test.reg.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import java.time.LocalTime;
import test.reg.Attribute;
import test.reg.Person;


@ImplementedBy(AttributeTimeRule.AttributeTimeRuleDefault.class)
public abstract class AttributeTimeRule implements ReportFunction<Person, LocalTime> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public LocalTime evaluate(Person input) {
		LocalTime output = doEvaluate(input);
		
		return output;
	}

	protected abstract LocalTime doEvaluate(Person input);

	public static class AttributeTimeRuleDefault extends AttributeTimeRule {
		@Override
		protected LocalTime doEvaluate(Person input) {
			LocalTime output = null;
			return assignOutput(output, input);
		}
		
		protected LocalTime assignOutput(LocalTime output, Person input) {
			output = MapperS.of(input)
				.mapSingleToItem(item -> item.<Attribute>map("getAttribute", person -> person.getAttribute()).<LocalTime>map("getHeroTime", attribute -> attribute.getHeroTime())).get();
			
			return output;
		}
	}
}
