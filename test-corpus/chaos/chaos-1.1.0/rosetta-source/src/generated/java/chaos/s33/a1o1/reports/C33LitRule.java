package chaos.s33.a1o1.reports;

import chaos.s33.a1o1.C33Trade;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.expression.CardinalityOperator;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;

import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.*;

@ImplementedBy(C33LitRule.C33LitRuleDefault.class)
public abstract class C33LitRule implements ReportFunction<C33Trade, String> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public String evaluate(C33Trade input) {
		String output = doEvaluate(input);
		
		return output;
	}

	protected abstract String doEvaluate(C33Trade input);

	public static class C33LitRuleDefault extends C33LitRule {
		@Override
		protected String doEvaluate(C33Trade input) {
			String output = null;
			return assignOutput(output, input);
		}
		
		protected String assignOutput(String output, C33Trade input) {
			output = MapperS.of(input)
				.mapSingleToItem(item -> {
					if (areEqual(item.<String>map("getUtid", c33Trade -> c33Trade.getUtid()), MapperS.of("tab\there"), CardinalityOperator.All).getOrDefault(false)) {
						return MapperS.of("\u00B5-yes \"y\"");
					}
					return MapperS.of(item.<String>map("getUtid", c33Trade -> c33Trade.getUtid()).getOrDefault("back\\slash"));
				}).get();
			
			return output;
		}
	}
}
