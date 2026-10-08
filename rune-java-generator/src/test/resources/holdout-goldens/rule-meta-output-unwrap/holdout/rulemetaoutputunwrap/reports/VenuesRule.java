package holdout.rulemetaoutputunwrap.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import com.rosetta.model.metafields.FieldWithMetaString;
import holdout.rulemetaoutputunwrap.Trade;
import holdout.rulemetaoutputunwrap.functions.VenuesOf;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;


@ImplementedBy(VenuesRule.VenuesRuleDefault.class)
public abstract class VenuesRule implements ReportFunction<Trade, List<String>> {
	
	// RosettaFunction dependencies
	//
	@Inject protected VenuesOf venuesOf;

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public List<String> evaluate(Trade input) {
		List<String> output = doEvaluate(input);
		
		return output;
	}

	protected abstract List<String> doEvaluate(Trade input);

	public static class VenuesRuleDefault extends VenuesRule {
		@Override
		protected List<String> doEvaluate(Trade input) {
			List<String> output = new ArrayList<>();
			return assignOutput(output, input);
		}
		
		protected List<String> assignOutput(List<String> output, Trade input) {
			output = MapperS.of(input)
				.mapSingleToList(item -> MapperC.<FieldWithMetaString>of(venuesOf.evaluate(item.get()))).<String>map("Type coercion", fieldWithMetaString -> fieldWithMetaString.getValue()).getMulti();
			
			return output;
		}
	}
}
