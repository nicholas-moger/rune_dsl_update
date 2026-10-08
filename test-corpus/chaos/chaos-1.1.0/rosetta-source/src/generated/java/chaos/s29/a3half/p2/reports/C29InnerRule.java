package chaos.s29.a3half.p2.reports;

import chaos.s29.a3half.p1.C29Inner;
import chaos.s29.a3half.p1.C29Outer;
import chaos.s29.a3half.p1.util.C29InnerDeepPathUtil;
import chaos.s29.a3half.p1.util.C29OuterDeepPathUtil;
import chaos.s29.a3half.p2.C29Bag;
import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;


@ImplementedBy(C29InnerRule.C29InnerRuleDefault.class)
public abstract class C29InnerRule implements ReportFunction<C29Bag, List<String>> {
	
	// RosettaFunction dependencies
	//
	@Inject protected C29InnerDeepPathUtil c29InnerDeepPathUtil;
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

	public static class C29InnerRuleDefault extends C29InnerRule {
		@Override
		protected List<String> doEvaluate(C29Bag input) {
			List<String> output = new ArrayList<>();
			return assignOutput(output, input);
		}
		
		protected List<String> assignOutput(List<String> output, C29Bag input) {
			output = MapperS.of(input)
				.mapSingleToList(item -> item.<C29Outer>map("getOuter", c29Bag -> c29Bag.getOuter()).<C29Inner>mapC("chooseInners", c29Outer -> c29OuterDeepPathUtil.chooseInners(c29Outer)).<String>map("chooseDeep", c29Inner -> c29InnerDeepPathUtil.chooseDeep(c29Inner))).getMulti();
			
			return output;
		}
	}
}
