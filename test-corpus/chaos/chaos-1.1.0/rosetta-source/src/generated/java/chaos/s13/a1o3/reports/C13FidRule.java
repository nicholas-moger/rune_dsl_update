package chaos.s13.a1o3.reports;

import chaos.s13.a1o3.C13Fact;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;


@ImplementedBy(C13FidRule.C13FidRuleDefault.class)
public abstract class C13FidRule implements ReportFunction<C13Fact, String> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public String evaluate(C13Fact input) {
		String output = doEvaluate(input);
		
		return output;
	}

	protected abstract String doEvaluate(C13Fact input);

	public static class C13FidRuleDefault extends C13FidRule {
		@Override
		protected String doEvaluate(C13Fact input) {
			String output = null;
			return assignOutput(output, input);
		}
		
		protected String assignOutput(String output, C13Fact input) {
			output = MapperS.of(input)
				.mapSingleToItem(item -> item.<String>map("getFid", c13Fact -> c13Fact.getFid())).get();
			
			return output;
		}
	}
}
