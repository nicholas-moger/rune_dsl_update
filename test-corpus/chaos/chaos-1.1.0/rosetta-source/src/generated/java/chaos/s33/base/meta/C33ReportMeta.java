package chaos.s33.base.meta;

import chaos.s33.base.C33Report;
import chaos.s33.base.validation.C33ReportTypeFormatValidator;
import chaos.s33.base.validation.C33ReportValidator;
import chaos.s33.base.validation.exists.C33ReportOnlyExistsValidator;
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


/**
 * @version 1.0.0
 */
@RosettaMeta(model=C33Report.class)
public class C33ReportMeta implements RosettaMetaData<C33Report> {

	@Override
	public List<Validator<? super C33Report>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C33Report, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C33Report> validator(ValidatorFactory factory) {
		return factory.<C33Report>create(C33ReportValidator.class);
	}

	@Override
	public Validator<? super C33Report> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C33Report>create(C33ReportTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C33Report> validator() {
		return new C33ReportValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C33Report> typeFormatValidator() {
		return new C33ReportTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C33Report, Set<String>> onlyExistsValidator() {
		return new C33ReportOnlyExistsValidator();
	}
}
