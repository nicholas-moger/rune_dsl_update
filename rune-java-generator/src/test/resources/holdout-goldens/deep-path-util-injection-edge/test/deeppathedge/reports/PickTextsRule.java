package test.deeppathedge.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import test.deeppathedge.Deep;
import test.deeppathedge.Inner;
import test.deeppathedge.util.InnerDeepPathUtil;


@ImplementedBy(PickTextsRule.PickTextsRuleDefault.class)
public abstract class PickTextsRule implements ReportFunction<Deep, List<String>> {
	
	// RosettaFunction dependencies
	//
	@Inject protected InnerDeepPathUtil innerDeepPathUtil;

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public List<String> evaluate(Deep input) {
		List<String> output = doEvaluate(input);
		
		return output;
	}

	protected abstract List<String> doEvaluate(Deep input);

	public static class PickTextsRuleDefault extends PickTextsRule {
		@Override
		protected List<String> doEvaluate(Deep input) {
			List<String> output = new ArrayList<>();
			return assignOutput(output, input);
		}
		
		protected List<String> assignOutput(List<String> output, Deep input) {
			final MapperC<Inner> thenArg = MapperS.of(input)
				.mapSingleToList(item -> item.<Inner>mapC("getPicks", deep -> deep.getPicks()));
			output = thenArg
				.mapItem(item -> item.<String>map("chooseCommon", inner -> innerDeepPathUtil.chooseCommon(inner))).getMulti();
			
			return output;
		}
	}
}
