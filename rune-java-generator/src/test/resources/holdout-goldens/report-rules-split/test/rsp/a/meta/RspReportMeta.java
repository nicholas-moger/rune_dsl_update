package test.rsp.a.meta;

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
import test.rsp.a.RspReport;
import test.rsp.a.validation.RspReportTypeFormatValidator;
import test.rsp.a.validation.RspReportValidator;
import test.rsp.a.validation.exists.RspReportOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=RspReport.class)
public class RspReportMeta implements RosettaMetaData<RspReport> {

	@Override
	public List<Validator<? super RspReport>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super RspReport, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super RspReport> validator(ValidatorFactory factory) {
		return factory.<RspReport>create(RspReportValidator.class);
	}

	@Override
	public Validator<? super RspReport> typeFormatValidator(ValidatorFactory factory) {
		return factory.<RspReport>create(RspReportTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super RspReport> validator() {
		return new RspReportValidator();
	}

	@Deprecated
	@Override
	public Validator<? super RspReport> typeFormatValidator() {
		return new RspReportTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super RspReport, Set<String>> onlyExistsValidator() {
		return new RspReportOnlyExistsValidator();
	}
}
