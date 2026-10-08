package test.rwq.b.meta;

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
import test.rwq.b.RwqReport;
import test.rwq.b.validation.RwqReportTypeFormatValidator;
import test.rwq.b.validation.RwqReportValidator;
import test.rwq.b.validation.exists.RwqReportOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=RwqReport.class)
public class RwqReportMeta implements RosettaMetaData<RwqReport> {

	@Override
	public List<Validator<? super RwqReport>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super RwqReport, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super RwqReport> validator(ValidatorFactory factory) {
		return factory.<RwqReport>create(RwqReportValidator.class);
	}

	@Override
	public Validator<? super RwqReport> typeFormatValidator(ValidatorFactory factory) {
		return factory.<RwqReport>create(RwqReportTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super RwqReport> validator() {
		return new RwqReportValidator();
	}

	@Deprecated
	@Override
	public Validator<? super RwqReport> typeFormatValidator() {
		return new RwqReportTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super RwqReport, Set<String>> onlyExistsValidator() {
		return new RwqReportOnlyExistsValidator();
	}
}
