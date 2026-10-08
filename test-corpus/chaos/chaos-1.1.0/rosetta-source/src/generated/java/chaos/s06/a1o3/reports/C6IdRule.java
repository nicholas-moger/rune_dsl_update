package chaos.s06.a1o3.reports;

import chaos.s06.a1o3.C6Event;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;


@ImplementedBy(C6IdRule.C6IdRuleDefault.class)
public abstract class C6IdRule implements ReportFunction<C6Event, String> {

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

	public static class C6IdRuleDefault extends C6IdRule {
		@Override
		protected String doEvaluate(C6Event input) {
			String output = null;
			return assignOutput(output, input);
		}
		
		protected String assignOutput(String output, C6Event input) {
			output = MapperS.of(input)
				.mapSingleToItem(item -> item.<String>map("getEventId", c6Event -> c6Event.getEventId())).get();
			
			return output;
		}
	}
}
