package chaos.s20.a2qual.meta;

import chaos.s20.a2qual.C20Twig;
import chaos.s20.a2qual.validation.C20TwigTypeFormatValidator;
import chaos.s20.a2qual.validation.C20TwigValidator;
import chaos.s20.a2qual.validation.exists.C20TwigOnlyExistsValidator;
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
@RosettaMeta(model=C20Twig.class)
public class C20TwigMeta implements RosettaMetaData<C20Twig> {

	@Override
	public List<Validator<? super C20Twig>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C20Twig, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C20Twig> validator(ValidatorFactory factory) {
		return factory.<C20Twig>create(C20TwigValidator.class);
	}

	@Override
	public Validator<? super C20Twig> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C20Twig>create(C20TwigTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C20Twig> validator() {
		return new C20TwigValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C20Twig> typeFormatValidator() {
		return new C20TwigTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C20Twig, Set<String>> onlyExistsValidator() {
		return new C20TwigOnlyExistsValidator();
	}
}
