package chaos.s07.base.reports;

import chaos.s07.base.C7Trade;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;


@ImplementedBy(C7UtidRule.C7UtidRuleDefault.class)
public abstract class C7UtidRule implements ReportFunction<C7Trade, String> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public String evaluate(C7Trade input) {
		String output = doEvaluate(input);
		
		return output;
	}

	protected abstract String doEvaluate(C7Trade input);

	public static class C7UtidRuleDefault extends C7UtidRule {
		@Override
		protected String doEvaluate(C7Trade input) {
			String output = null;
			return assignOutput(output, input);
		}
		
		protected String assignOutput(String output, C7Trade input) {
			output = MapperS.of(input)
				.mapSingleToItem(item -> item.<String>map("getUtid", c7Trade -> c7Trade.getUtid())).get();
			
			return output;
		}
	}
}
