package chaos.s28.a2dangle.meta;

import chaos.s28.a2dangle.C28Report;
import chaos.s28.a2dangle.validation.C28ReportTypeFormatValidator;
import chaos.s28.a2dangle.validation.C28ReportValidator;
import chaos.s28.a2dangle.validation.exists.C28ReportOnlyExistsValidator;
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
@RosettaMeta(model=C28Report.class)
public class C28ReportMeta implements RosettaMetaData<C28Report> {

	@Override
	public List<Validator<? super C28Report>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C28Report, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C28Report> validator(ValidatorFactory factory) {
		return factory.<C28Report>create(C28ReportValidator.class);
	}

	@Override
	public Validator<? super C28Report> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C28Report>create(C28ReportTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C28Report> validator() {
		return new C28ReportValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C28Report> typeFormatValidator() {
		return new C28ReportTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C28Report, Set<String>> onlyExistsValidator() {
		return new C28ReportOnlyExistsValidator();
	}
}
