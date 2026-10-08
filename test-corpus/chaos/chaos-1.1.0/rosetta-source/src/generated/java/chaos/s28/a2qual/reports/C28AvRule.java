package chaos.s28.a2qual.reports;

import chaos.s28.a2qual.C28OptA;
import chaos.s28.a2qual.C28Trade;
import chaos.s28.a2qual.C28Which;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;


@ImplementedBy(C28AvRule.C28AvRuleDefault.class)
public abstract class C28AvRule implements ReportFunction<C28Trade, String> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public String evaluate(C28Trade input) {
		String output = doEvaluate(input);
		
		return output;
	}

	protected abstract String doEvaluate(C28Trade input);

	public static class C28AvRuleDefault extends C28AvRule {
		@Override
		protected String doEvaluate(C28Trade input) {
			String output = null;
			return assignOutput(output, input);
		}
		
		protected String assignOutput(String output, C28Trade input) {
			output = MapperS.of(input)
				.mapSingleToItem(item -> item.<C28Which>map("getWhich", c28Trade -> c28Trade.getWhich()).<C28OptA>map("getC28OptA", c28Which -> c28Which.getC28OptA()).<String>map("getAv", c28OptA -> c28OptA.getAv())).get();
			
			return output;
		}
	}
}
