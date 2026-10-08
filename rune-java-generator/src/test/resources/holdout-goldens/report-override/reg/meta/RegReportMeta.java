package reg.meta;

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
import reg.RegReport;
import reg.validation.RegReportTypeFormatValidator;
import reg.validation.RegReportValidator;
import reg.validation.exists.RegReportOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=RegReport.class)
public class RegReportMeta implements RosettaMetaData<RegReport> {

	@Override
	public List<Validator<? super RegReport>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super RegReport, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super RegReport> validator(ValidatorFactory factory) {
		return factory.<RegReport>create(RegReportValidator.class);
	}

	@Override
	public Validator<? super RegReport> typeFormatValidator(ValidatorFactory factory) {
		return factory.<RegReport>create(RegReportTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super RegReport> validator() {
		return new RegReportValidator();
	}

	@Deprecated
	@Override
	public Validator<? super RegReport> typeFormatValidator() {
		return new RegReportTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super RegReport, Set<String>> onlyExistsValidator() {
		return new RegReportOnlyExistsValidator();
	}
}
