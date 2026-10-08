package test.reg.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import java.util.ArrayList;
import java.util.List;
import test.reg.Person;
import test.reg.PowerEnum;


@ImplementedBy(PowersRule.PowersRuleDefault.class)
public abstract class PowersRule implements ReportFunction<Person, List<PowerEnum>> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public List<PowerEnum> evaluate(Person input) {
		List<PowerEnum> output = doEvaluate(input);
		
		return output;
	}

	protected abstract List<PowerEnum> doEvaluate(Person input);

	public static class PowersRuleDefault extends PowersRule {
		@Override
		protected List<PowerEnum> doEvaluate(Person input) {
			List<PowerEnum> output = new ArrayList<>();
			return assignOutput(output, input);
		}
		
		protected List<PowerEnum> assignOutput(List<PowerEnum> output, Person input) {
			output = MapperS.of(input)
				.mapSingleToList(item -> item.<PowerEnum>mapC("getPowers", person -> person.getPowers())).getMulti();
			
			return output;
		}
	}
}
