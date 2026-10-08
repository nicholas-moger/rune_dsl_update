package test.rws.b.meta;

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
import test.rws.b.RwsReport;
import test.rws.b.validation.RwsReportTypeFormatValidator;
import test.rws.b.validation.RwsReportValidator;
import test.rws.b.validation.exists.RwsReportOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=RwsReport.class)
public class RwsReportMeta implements RosettaMetaData<RwsReport> {

	@Override
	public List<Validator<? super RwsReport>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super RwsReport, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super RwsReport> validator(ValidatorFactory factory) {
		return factory.<RwsReport>create(RwsReportValidator.class);
	}

	@Override
	public Validator<? super RwsReport> typeFormatValidator(ValidatorFactory factory) {
		return factory.<RwsReport>create(RwsReportTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super RwsReport> validator() {
		return new RwsReportValidator();
	}

	@Deprecated
	@Override
	public Validator<? super RwsReport> typeFormatValidator() {
		return new RwsReportTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super RwsReport, Set<String>> onlyExistsValidator() {
		return new RwsReportOnlyExistsValidator();
	}
}
