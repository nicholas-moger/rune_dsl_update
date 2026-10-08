package chaos.s06.a2wild.reports;

import chaos.s06.a2wild.C6Event;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;


@ImplementedBy(C6FirstRule.C6FirstRuleDefault.class)
public abstract class C6FirstRule implements ReportFunction<C6Event, String> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public String evaluate(C6Event input) {
		String output = doEvaluate(input);
		
		return output;
	}

	protected abstract String doEvaluate(C6Event input);

	public static class C6FirstRuleDefault extends C6FirstRule {
		@Override
		protected String doEvaluate(C6Event input) {
			String output = null;
			return assignOutput(output, input);
		}
		
		protected String assignOutput(String output, C6Event input) {
			output = MapperS.of(input)
				.mapSingleToItem(item -> MapperS.of(item.<String>mapC("getLegs", c6Event -> c6Event.getLegs())
					.first().getOrDefault("none"))).get();
			
			return output;
		}
	}
}
