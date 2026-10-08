package ext.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import ext.ExtRegReport;
import ext.validation.ExtRegReportTypeFormatValidator;
import ext.validation.ExtRegReportValidator;
import ext.validation.exists.ExtRegReportOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=ExtRegReport.class)
public class ExtRegReportMeta implements RosettaMetaData<ExtRegReport> {

	@Override
	public List<Validator<? super ExtRegReport>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super ExtRegReport, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super ExtRegReport> validator(ValidatorFactory factory) {
		return factory.<ExtRegReport>create(ExtRegReportValidator.class);
	}

	@Override
	public Validator<? super ExtRegReport> typeFormatValidator(ValidatorFactory factory) {
		return factory.<ExtRegReport>create(ExtRegReportTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super ExtRegReport> validator() {
		return new ExtRegReportValidator();
	}

	@Deprecated
	@Override
	public Validator<? super ExtRegReport> typeFormatValidator() {
		return new ExtRegReportTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super ExtRegReport, Set<String>> onlyExistsValidator() {
		return new ExtRegReportOnlyExistsValidator();
	}
}
