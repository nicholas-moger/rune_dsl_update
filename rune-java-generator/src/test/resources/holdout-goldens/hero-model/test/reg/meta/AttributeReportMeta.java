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
import test.reg.AttributeReport;
import test.reg.validation.AttributeReportTypeFormatValidator;
import test.reg.validation.AttributeReportValidator;
import test.reg.validation.exists.AttributeReportOnlyExistsValidator;


/**
 * @version test
 */
@RosettaMeta(model=AttributeReport.class)
public class AttributeReportMeta implements RosettaMetaData<AttributeReport> {

	@Override
	public List<Validator<? super AttributeReport>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super AttributeReport, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super AttributeReport> validator(ValidatorFactory factory) {
		return factory.<AttributeReport>create(AttributeReportValidator.class);
	}

	@Override
	public Validator<? super AttributeReport> typeFormatValidator(ValidatorFactory factory) {
		return factory.<AttributeReport>create(AttributeReportTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super AttributeReport> validator() {
		return new AttributeReportValidator();
	}

	@Deprecated
	@Override
	public Validator<? super AttributeReport> typeFormatValidator() {
		return new AttributeReportTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super AttributeReport, Set<String>> onlyExistsValidator() {
		return new AttributeReportOnlyExistsValidator();
	}
}
