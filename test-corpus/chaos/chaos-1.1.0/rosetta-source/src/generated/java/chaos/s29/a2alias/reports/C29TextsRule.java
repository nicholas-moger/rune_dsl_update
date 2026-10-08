package chaos.s29.a2alias.reports;

import chaos.s29.a2alias.C29Bag;
import chaos.s29.a2alias.C29Outer;
import chaos.s29.a2alias.util.C29OuterDeepPathUtil;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;


@ImplementedBy(C29TextsRule.C29TextsRuleDefault.class)
public abstract class C29TextsRule implements ReportFunction<C29Bag, List<String>> {
	
	// RosettaFunction dependencies
	//
	@Inject protected C29OuterDeepPathUtil c29OuterDeepPathUtil;

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public List<String> evaluate(C29Bag input) {
		List<String> output = doEvaluate(input);
		
		return output;
	}

	protected abstract List<String> doEvaluate(C29Bag input);

	public static class C29TextsRuleDefault extends C29TextsRule {
		@Override
		protected List<String> doEvaluate(C29Bag input) {
			List<String> output = new ArrayList<>();
			return assignOutput(output, input);
		}
		
		protected List<String> assignOutput(List<String> output, C29Bag input) {
			final MapperC<C29Outer> thenArg = MapperS.of(input)
				.mapSingleToList(item -> item.<C29Outer>mapC("getOuters", c29Bag -> c29Bag.getOuters()));
			output = thenArg
				.mapItem(item -> item.<String>map("chooseText", c29Outer -> c29OuterDeepPathUtil.chooseText(c29Outer))).getMulti();
			
			return output;
		}
	}
}
