package test.rsr.b.meta;

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
import test.rsr.b.RsrReport;
import test.rsr.b.validation.RsrReportTypeFormatValidator;
import test.rsr.b.validation.RsrReportValidator;
import test.rsr.b.validation.exists.RsrReportOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=RsrReport.class)
public class RsrReportMeta implements RosettaMetaData<RsrReport> {

	@Override
	public List<Validator<? super RsrReport>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super RsrReport, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super RsrReport> validator(ValidatorFactory factory) {
		return factory.<RsrReport>create(RsrReportValidator.class);
	}

	@Override
	public Validator<? super RsrReport> typeFormatValidator(ValidatorFactory factory) {
		return factory.<RsrReport>create(RsrReportTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super RsrReport> validator() {
		return new RsrReportValidator();
	}

	@Deprecated
	@Override
	public Validator<? super RsrReport> typeFormatValidator() {
		return new RsrReportTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super RsrReport, Set<String>> onlyExistsValidator() {
		return new RsrReportOnlyExistsValidator();
	}
}
