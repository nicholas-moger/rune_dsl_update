package test.reg.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import java.time.ZonedDateTime;
import test.reg.Attribute;
import test.reg.Person;


@ImplementedBy(AttributeZonedDateTimeRule.AttributeZonedDateTimeRuleDefault.class)
public abstract class AttributeZonedDateTimeRule implements ReportFunction<Person, ZonedDateTime> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public ZonedDateTime evaluate(Person input) {
		ZonedDateTime output = doEvaluate(input);
		
		return output;
	}

	protected abstract ZonedDateTime doEvaluate(Person input);

	public static class AttributeZonedDateTimeRuleDefault extends AttributeZonedDateTimeRule {
		@Override
		protected ZonedDateTime doEvaluate(Person input) {
			ZonedDateTime output = null;
			return assignOutput(output, input);
		}
		
		protected ZonedDateTime assignOutput(ZonedDateTime output, Person input) {
			output = MapperS.of(input)
				.mapSingleToItem(item -> item.<Attribute>map("getAttribute", person -> person.getAttribute()).<ZonedDateTime>map("getHeroZonedDateTime", attribute -> attribute.getHeroZonedDateTime())).get();
			
			return output;
		}
	}
}
