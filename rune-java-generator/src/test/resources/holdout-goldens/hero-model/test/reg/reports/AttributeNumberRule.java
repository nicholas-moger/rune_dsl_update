package test.reg.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import java.math.BigDecimal;
import test.reg.Attribute;
import test.reg.Person;


@ImplementedBy(AttributeNumberRule.AttributeNumberRuleDefault.class)
public abstract class AttributeNumberRule implements ReportFunction<Person, BigDecimal> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public BigDecimal evaluate(Person input) {
		BigDecimal output = doEvaluate(input);
		
		return output;
	}

	protected abstract BigDecimal doEvaluate(Person input);

	public static class AttributeNumberRuleDefault extends AttributeNumberRule {
		@Override
		protected BigDecimal doEvaluate(Person input) {
			BigDecimal output = null;
			return assignOutput(output, input);
		}
		
		protected BigDecimal assignOutput(BigDecimal output, Person input) {
			output = MapperS.of(input)
				.mapSingleToItem(item -> item.<Attribute>map("getAttribute", person -> person.getAttribute()).<BigDecimal>map("getHeroNumber", attribute -> attribute.getHeroNumber())).get();
			
			return output;
		}
	}
}
