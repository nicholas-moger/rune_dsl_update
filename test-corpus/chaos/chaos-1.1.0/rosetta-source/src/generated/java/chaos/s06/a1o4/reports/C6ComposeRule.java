package chaos.s06.a1o4.reports;

import chaos.s06.a1o4.C6Event;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.MapperMaths;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import javax.inject.Inject;


@ImplementedBy(C6ComposeRule.C6ComposeRuleDefault.class)
public abstract class C6ComposeRule implements ReportFunction<C6Event, String> {
	
	// RosettaFunction dependencies
	//
	@Inject protected C6IdRule c6IdRule;

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

	public static class C6ComposeRuleDefault extends C6ComposeRule {
		@Override
		protected String doEvaluate(C6Event input) {
			String output = null;
			return assignOutput(output, input);
		}
		
		protected String assignOutput(String output, C6Event input) {
			final MapperS<String> thenArg = MapperS.of(input)
				.mapSingleToItem(item -> MapperS.of(c6IdRule.evaluate(item.get())));
			output = thenArg
				.mapSingleToItem(item -> MapperMaths.<String, String, String>add(item, MapperS.of("-composed"))).get();
			
			return output;
		}
	}
}
