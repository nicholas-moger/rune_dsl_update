package chaos.s28.a1o2.meta;

import chaos.s28.a1o2.C28ReportChoice;
import chaos.s28.a1o2.validation.C28ReportChoiceTypeFormatValidator;
import chaos.s28.a1o2.validation.C28ReportChoiceValidator;
import chaos.s28.a1o2.validation.datarule.C28ReportChoiceChoice;
import chaos.s28.a1o2.validation.exists.C28ReportChoiceOnlyExistsValidator;
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
@RosettaMeta(model=C28ReportChoice.class)
public class C28ReportChoiceMeta implements RosettaMetaData<C28ReportChoice> {

	@Override
	public List<Validator<? super C28ReportChoice>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
			factory.<C28ReportChoice>create(C28ReportChoiceChoice.class)
		);
	}
	
	@Override
	public List<Function<? super C28ReportChoice, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C28ReportChoice> validator(ValidatorFactory factory) {
		return factory.<C28ReportChoice>create(C28ReportChoiceValidator.class);
	}

	@Override
	public Validator<? super C28ReportChoice> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C28ReportChoice>create(C28ReportChoiceTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C28ReportChoice> validator() {
		return new C28ReportChoiceValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C28ReportChoice> typeFormatValidator() {
		return new C28ReportChoiceTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C28ReportChoice, Set<String>> onlyExistsValidator() {
		return new C28ReportChoiceOnlyExistsValidator();
	}
}
