package common.layer.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import common.layer.CommonReport;
import common.layer.validation.CommonReportTypeFormatValidator;
import common.layer.validation.CommonReportValidator;
import common.layer.validation.exists.CommonReportOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=CommonReport.class)
public class CommonReportMeta implements RosettaMetaData<CommonReport> {

	@Override
	public List<Validator<? super CommonReport>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super CommonReport, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super CommonReport> validator(ValidatorFactory factory) {
		return factory.<CommonReport>create(CommonReportValidator.class);
	}

	@Override
	public Validator<? super CommonReport> typeFormatValidator(ValidatorFactory factory) {
		return factory.<CommonReport>create(CommonReportTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super CommonReport> validator() {
		return new CommonReportValidator();
	}

	@Deprecated
	@Override
	public Validator<? super CommonReport> typeFormatValidator() {
		return new CommonReportTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super CommonReport, Set<String>> onlyExistsValidator() {
		return new CommonReportOnlyExistsValidator();
	}
}
