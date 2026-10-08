package test.reg.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import test.reg.Organisation;


@ImplementedBy(OrganisationNameRule.OrganisationNameRuleDefault.class)
public abstract class OrganisationNameRule implements ReportFunction<Organisation, String> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public String evaluate(Organisation input) {
		String output = doEvaluate(input);
		
		return output;
	}

	protected abstract String doEvaluate(Organisation input);

	public static class OrganisationNameRuleDefault extends OrganisationNameRule {
		@Override
		protected String doEvaluate(Organisation input) {
			String output = null;
			return assignOutput(output, input);
		}
		
		protected String assignOutput(String output, Organisation input) {
			output = MapperS.of(input)
				.mapSingleToItem(item -> item.<String>map("getName", organisation -> organisation.getName())).get();
			
			return output;
		}
	}
}
