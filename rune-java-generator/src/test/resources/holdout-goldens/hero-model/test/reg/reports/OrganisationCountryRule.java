package test.reg.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import test.reg.CountryEnum;
import test.reg.Organisation;


@ImplementedBy(OrganisationCountryRule.OrganisationCountryRuleDefault.class)
public abstract class OrganisationCountryRule implements ReportFunction<Organisation, CountryEnum> {

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public CountryEnum evaluate(Organisation input) {
		CountryEnum output = doEvaluate(input);
		
		return output;
	}

	protected abstract CountryEnum doEvaluate(Organisation input);

	public static class OrganisationCountryRuleDefault extends OrganisationCountryRule {
		@Override
		protected CountryEnum doEvaluate(Organisation input) {
			CountryEnum output = null;
			return assignOutput(output, input);
		}
		
		protected CountryEnum assignOutput(CountryEnum output, Organisation input) {
			output = MapperS.of(input)
				.mapSingleToItem(item -> item.<CountryEnum>map("getCountry", organisation -> organisation.getCountry())).get();
			
			return output;
		}
	}
}
