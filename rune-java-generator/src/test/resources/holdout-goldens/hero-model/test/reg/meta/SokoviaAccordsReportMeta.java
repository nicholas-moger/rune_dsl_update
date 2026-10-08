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
import test.reg.SokoviaAccordsReport;
import test.reg.validation.SokoviaAccordsReportTypeFormatValidator;
import test.reg.validation.SokoviaAccordsReportValidator;
import test.reg.validation.exists.SokoviaAccordsReportOnlyExistsValidator;


/**
 * @version test
 */
@RosettaMeta(model=SokoviaAccordsReport.class)
public class SokoviaAccordsReportMeta implements RosettaMetaData<SokoviaAccordsReport> {

	@Override
	public List<Validator<? super SokoviaAccordsReport>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super SokoviaAccordsReport, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super SokoviaAccordsReport> validator(ValidatorFactory factory) {
		return factory.<SokoviaAccordsReport>create(SokoviaAccordsReportValidator.class);
	}

	@Override
	public Validator<? super SokoviaAccordsReport> typeFormatValidator(ValidatorFactory factory) {
		return factory.<SokoviaAccordsReport>create(SokoviaAccordsReportTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super SokoviaAccordsReport> validator() {
		return new SokoviaAccordsReportValidator();
	}

	@Deprecated
	@Override
	public Validator<? super SokoviaAccordsReport> typeFormatValidator() {
		return new SokoviaAccordsReportTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super SokoviaAccordsReport, Set<String>> onlyExistsValidator() {
		return new SokoviaAccordsReportOnlyExistsValidator();
	}
}
