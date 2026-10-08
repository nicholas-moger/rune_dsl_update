package chaos.s07.a2qual.meta;

import chaos.s07.a2qual.C7Report;
import chaos.s07.a2qual.validation.C7ReportTypeFormatValidator;
import chaos.s07.a2qual.validation.C7ReportValidator;
import chaos.s07.a2qual.validation.exists.C7ReportOnlyExistsValidator;
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
@RosettaMeta(model=C7Report.class)
public class C7ReportMeta implements RosettaMetaData<C7Report> {

	@Override
	public List<Validator<? super C7Report>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C7Report, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C7Report> validator(ValidatorFactory factory) {
		return factory.<C7Report>create(C7ReportValidator.class);
	}

	@Override
	public Validator<? super C7Report> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C7Report>create(C7ReportTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C7Report> validator() {
		return new C7ReportValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C7Report> typeFormatValidator() {
		return new C7ReportTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C7Report, Set<String>> onlyExistsValidator() {
		return new C7ReportOnlyExistsValidator();
	}
}
