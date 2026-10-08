package chaos.s26.a1o4.reports;

import chaos.s26.a1o4.C26Bag;
import chaos.s26.a1o4.C26KindEnum;
import chaos.s26.a1o4.functions.C26Name;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import javax.inject.Inject;


@ImplementedBy(C26KindRule.C26KindRuleDefault.class)
public abstract class C26KindRule implements ReportFunction<C26Bag, String> {
	
	// RosettaFunction dependencies
	//
	@Inject protected C26Name c26Name;

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public String evaluate(C26Bag input) {
		String output = doEvaluate(input);
		
		return output;
	}

	protected abstract String doEvaluate(C26Bag input);

	public static class C26KindRuleDefault extends C26KindRule {
		@Override
		protected String doEvaluate(C26Bag input) {
			String output = null;
			return assignOutput(output, input);
		}
		
		protected String assignOutput(String output, C26Bag input) {
			final MapperS<C26KindEnum> thenArg = MapperS.of(input)
				.mapSingleToItem(item -> item.<C26KindEnum>map("getKind", c26Bag -> c26Bag.getKind()));
			output = thenArg
				.mapSingleToItem(item -> {
					final C26KindEnum switchArgument = item.get();
					if (switchArgument == null) {
						return MapperS.<String>ofNull();
					}
					if (switchArgument == C26KindEnum.RED) {
						return MapperS.of(c26Name.evaluate(item.get()));
					}
					return MapperS.of("d");
				}).get();
			
			return output;
		}
	}
}
