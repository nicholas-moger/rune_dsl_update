package test.rsh.a.meta;

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
import test.rsh.a.RshReport;
import test.rsh.a.validation.RshReportTypeFormatValidator;
import test.rsh.a.validation.RshReportValidator;
import test.rsh.a.validation.exists.RshReportOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=RshReport.class)
public class RshReportMeta implements RosettaMetaData<RshReport> {

	@Override
	public List<Validator<? super RshReport>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super RshReport, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super RshReport> validator(ValidatorFactory factory) {
		return factory.<RshReport>create(RshReportValidator.class);
	}

	@Override
	public Validator<? super RshReport> typeFormatValidator(ValidatorFactory factory) {
		return factory.<RshReport>create(RshReportTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super RshReport> validator() {
		return new RshReportValidator();
	}

	@Deprecated
	@Override
	public Validator<? super RshReport> typeFormatValidator() {
		return new RshReportTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super RshReport, Set<String>> onlyExistsValidator() {
		return new RshReportOnlyExistsValidator();
	}
}
