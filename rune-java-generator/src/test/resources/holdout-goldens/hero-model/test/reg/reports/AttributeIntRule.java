package test.reg.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import test.reg.Attribute;
import test.reg.Person;


@ImplementedBy(AttributeIntRule.AttributeIntRuleDefault.class)
public abstract class AttributeIntRule implements ReportFunction<Person, Integer> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public Integer evaluate(Person input) {
		Integer output = doEvaluate(input);
		
		return output;
	}

	protected abstract Integer doEvaluate(Person input);

	public static class AttributeIntRuleDefault extends AttributeIntRule {
		@Override
		protected Integer doEvaluate(Person input) {
			Integer output = null;
			return assignOutput(output, input);
		}
		
		protected Integer assignOutput(Integer output, Person input) {
			output = MapperS.of(input)
				.mapSingleToItem(item -> item.<Attribute>map("getAttribute", person -> person.getAttribute()).<Integer>map("getHeroInt", attribute -> attribute.getHeroInt())).get();
			
			return output;
		}
	}
}
