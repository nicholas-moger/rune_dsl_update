package test.reg.reports;

import com.google.inject.ImplementedBy;
import com.rosetta.model.lib.annotations.RosettaReport;
import com.rosetta.model.lib.annotations.RuneLabelProvider;
import com.rosetta.model.lib.functions.ModelObjectValidator;
import com.rosetta.model.lib.reports.ReportFunction;
import java.util.Optional;
import javax.inject.Inject;
import test.reg.Person;
import test.reg.SokoviaAccordsReport;
import test.reg.labels.ShieldAvengersSokoviaAccordsLabelProvider;


@RosettaReport(namespace="test.reg", body="Shield", corpusList={"Avengers", "SokoviaAccords"})
@RuneLabelProvider(labelProvider=ShieldAvengersSokoviaAccordsLabelProvider.class)
@ImplementedBy(ShieldAvengersSokoviaAccordsReportFunction.ShieldAvengersSokoviaAccordsReportFunctionDefault.class)
public abstract class ShieldAvengersSokoviaAccordsReportFunction implements ReportFunction<Person, SokoviaAccordsReport> {
	
	@Inject protected ModelObjectValidator objectValidator;
	
	// RosettaFunction dependencies
	//
	@Inject protected AttributeIntRule attributeIntRule;
	@Inject protected AttributeNumberRule attributeNumberRule;
	@Inject protected AttributeTimeRule attributeTimeRule;
	@Inject protected AttributeZonedDateTimeRule attributeZonedDateTimeRule;
	@Inject protected DateOfBirthRule dateOfBirthRule;
	@Inject protected HeroNameRule heroNameRule;
	@Inject protected HeroOrganisationsRule heroOrganisationsRule;
	@Inject protected NationalityRule nationalityRule;
	@Inject protected NotModelledRule notModelledRule;
	@Inject protected PowersRule powersRule;
	@Inject protected SpecialAbilitiesRule specialAbilitiesRule;

	/**
	* @param input 
	* @return output 
	*/
	@Override
	public SokoviaAccordsReport evaluate(Person input) {
		SokoviaAccordsReport.SokoviaAccordsReportBuilder outputBuilder = doEvaluate(input);
		
		final SokoviaAccordsReport output;
		if (outputBuilder == null) {
			output = null;
		} else {
			output = outputBuilder.build();
			objectValidator.validate(SokoviaAccordsReport.class, output);
		}
		
		return output;
	}

	protected abstract SokoviaAccordsReport.SokoviaAccordsReportBuilder doEvaluate(Person input);

	public static class ShieldAvengersSokoviaAccordsReportFunctionDefault extends ShieldAvengersSokoviaAccordsReportFunction {
		@Override
		protected SokoviaAccordsReport.SokoviaAccordsReportBuilder doEvaluate(Person input) {
			SokoviaAccordsReport.SokoviaAccordsReportBuilder output = SokoviaAccordsReport.builder();
			return assignOutput(output, input);
		}
		
		protected SokoviaAccordsReport.SokoviaAccordsReportBuilder assignOutput(SokoviaAccordsReport.SokoviaAccordsReportBuilder output, Person input) {
			output
				.setHeroName(heroNameRule.evaluate(input));
			
			output
				.setDateOfBirth(dateOfBirthRule.evaluate(input));
			
			output
				.setNationality(nationalityRule.evaluate(input));
			
			output
				.setHasSpecialAbilities(specialAbilitiesRule.evaluate(input));
			
			output
				.setPowers(powersRule.evaluate(input));
			
			output
				.getOrCreateAttribute()
				.setHeroInt(attributeIntRule.evaluate(input));
			
			output
				.getOrCreateAttribute()
				.setHeroNumber(attributeNumberRule.evaluate(input));
			
			output
				.getOrCreateAttribute()
				.setHeroTime(attributeTimeRule.evaluate(input));
			
			output
				.getOrCreateAttribute()
				.setHeroZonedDateTime(attributeZonedDateTimeRule.evaluate(input));
			
			output
				.setOrganisations(heroOrganisationsRule.evaluate(input));
			
			output
				.setNotModelled(notModelledRule.evaluate(input));
			
			return Optional.ofNullable(output)
				.map(o -> o.prune())
				.orElse(null);
		}
	}
}
