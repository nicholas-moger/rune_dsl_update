package test.reg.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.mapper.MapperC;
import com.rosetta.model.lib.mapper.MapperS;
import com.rosetta.model.lib.reports.ReportFunction;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import test.reg.Organisation;
import test.reg.OrganisationReport;
import test.reg.Person;


@ImplementedBy(HeroOrganisationsRule.HeroOrganisationsRuleDefault.class)
public abstract class HeroOrganisationsRule implements ReportFunction<Person, List<? extends OrganisationReport>> {
	
	@Inject protected ModelObjectValidator objectValidator;
	
	// RosettaFunction dependencies
	//
	@Inject protected IsGovernmentAgencyRule isGovernmentAgencyRule;
	@Inject protected OrganisationCountryRule organisationCountryRule;
	@Inject protected OrganisationNameRule organisationNameRule;

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public List<? extends OrganisationReport> evaluate(Person input) {
		List<OrganisationReport.OrganisationReportBuilder> outputBuilder = doEvaluate(input);
		
		final List<? extends OrganisationReport> output;
		if (outputBuilder == null) {
			output = null;
		} else {
			output = outputBuilder.stream().map(OrganisationReport::build).collect(Collectors.toList());
			objectValidator.validate(OrganisationReport.class, output);
		}
		
		return output;
	}

	protected abstract List<OrganisationReport.OrganisationReportBuilder> doEvaluate(Person input);

	public static class HeroOrganisationsRuleDefault extends HeroOrganisationsRule {
		@Override
		protected List<OrganisationReport.OrganisationReportBuilder> doEvaluate(Person input) {
			List<OrganisationReport.OrganisationReportBuilder> output = new ArrayList<>();
			return assignOutput(output, input);
		}
		
		protected List<OrganisationReport.OrganisationReportBuilder> assignOutput(List<OrganisationReport.OrganisationReportBuilder> output, Person input) {
			final MapperC<Organisation> thenArg = MapperS.of(input)
				.mapSingleToList(item -> item.<Organisation>mapC("getOrganisations", person -> person.getOrganisations()));
			output = toBuilder(thenArg
				.mapItem(item -> MapperS.of(OrganisationReport.builder()
					.setName(organisationNameRule.evaluate(item.get()))
					.setCountry(organisationCountryRule.evaluate(item.get()))
					.setIsGovernmentAgency(isGovernmentAgencyRule.evaluate(item.get()))
					.build())).getMulti());
			
			return Optional.ofNullable(output)
				.map(o -> o.stream().map(i -> i.prune()).collect(Collectors.toList()))
				.orElse(null);
		}
	}
}
