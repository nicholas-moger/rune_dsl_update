package chaos.s21.base.meta;

import chaos.s21.base.C21Prefer;
import chaos.s21.base.validation.C21PreferTypeFormatValidator;
import chaos.s21.base.validation.C21PreferValidator;
import chaos.s21.base.validation.datarule.C21PreferC21Opt;
import chaos.s21.base.validation.datarule.C21PreferC21Req;
import chaos.s21.base.validation.exists.C21PreferOnlyExistsValidator;
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
@RosettaMeta(model=C21Prefer.class)
public class C21PreferMeta implements RosettaMetaData<C21Prefer> {

	@Override
	public List<Validator<? super C21Prefer>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
			factory.<C21Prefer>create(C21PreferC21Req.class),
			factory.<C21Prefer>create(C21PreferC21Opt.class)
		);
	}
	
	@Override
	public List<Function<? super C21Prefer, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C21Prefer> validator(ValidatorFactory factory) {
		return factory.<C21Prefer>create(C21PreferValidator.class);
	}

	@Override
	public Validator<? super C21Prefer> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C21Prefer>create(C21PreferTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C21Prefer> validator() {
		return new C21PreferValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C21Prefer> typeFormatValidator() {
		return new C21PreferTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C21Prefer, Set<String>> onlyExistsValidator() {
		return new C21PreferOnlyExistsValidator();
	}
}
