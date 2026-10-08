package test.reg.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import test.reg.Organisation;


@ImplementedBy(IsGovernmentAgencyRule.IsGovernmentAgencyRuleDefault.class)
public abstract class IsGovernmentAgencyRule implements ReportFunction<Organisation, Boolean> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public Boolean evaluate(Organisation input) {
		Boolean output = doEvaluate(input);
		
		return output;
	}

	protected abstract Boolean doEvaluate(Organisation input);

	public static class IsGovernmentAgencyRuleDefault extends IsGovernmentAgencyRule {
		@Override
		protected Boolean doEvaluate(Organisation input) {
			Boolean output = null;
			return assignOutput(output, input);
		}
		
		protected Boolean assignOutput(Boolean output, Organisation input) {
			output = MapperS.of(input)
				.mapSingleToItem(item -> item.<Boolean>map("getIsGovernmentAgency", organisation -> organisation.getIsGovernmentAgency())).get();
			
			return output;
		}
	}
}
