package test.reg.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import test.reg.OrganisationReport;
import test.reg.validation.OrganisationReportTypeFormatValidator;
import test.reg.validation.OrganisationReportValidator;
import test.reg.validation.exists.OrganisationReportOnlyExistsValidator;


/**
 * @version test
 */
@RosettaMeta(model=OrganisationReport.class)
public class OrganisationReportMeta implements RosettaMetaData<OrganisationReport> {

	@Override
	public List<Validator<? super OrganisationReport>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super OrganisationReport, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super OrganisationReport> validator(ValidatorFactory factory) {
		return factory.<OrganisationReport>create(OrganisationReportValidator.class);
	}

	@Override
	public Validator<? super OrganisationReport> typeFormatValidator(ValidatorFactory factory) {
		return factory.<OrganisationReport>create(OrganisationReportTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super OrganisationReport> validator() {
		return new OrganisationReportValidator();
	}

	@Deprecated
	@Override
	public Validator<? super OrganisationReport> typeFormatValidator() {
		return new OrganisationReportTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super OrganisationReport, Set<String>> onlyExistsValidator() {
		return new OrganisationReportOnlyExistsValidator();
	}
}
