package chaos.s21.a1o3.meta;

import chaos.s21.a1o3.C21Paths;
import chaos.s21.a1o3.validation.C21PathsTypeFormatValidator;
import chaos.s21.a1o3.validation.C21PathsValidator;
import chaos.s21.a1o3.validation.exists.C21PathsOnlyExistsValidator;
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
@RosettaMeta(model=C21Paths.class)
public class C21PathsMeta implements RosettaMetaData<C21Paths> {

	@Override
	public List<Validator<? super C21Paths>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C21Paths, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C21Paths> validator(ValidatorFactory factory) {
		return factory.<C21Paths>create(C21PathsValidator.class);
	}

	@Override
	public Validator<? super C21Paths> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C21Paths>create(C21PathsTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C21Paths> validator() {
		return new C21PathsValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C21Paths> typeFormatValidator() {
		return new C21PathsTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C21Paths, Set<String>> onlyExistsValidator() {
		return new C21PathsOnlyExistsValidator();
	}
}
