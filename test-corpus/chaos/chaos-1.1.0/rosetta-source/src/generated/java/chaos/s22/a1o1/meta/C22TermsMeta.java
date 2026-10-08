package chaos.s22.a1o1.meta;

import chaos.s22.a1o1.C22Terms;
import chaos.s22.a1o1.validation.C22TermsTypeFormatValidator;
import chaos.s22.a1o1.validation.C22TermsValidator;
import chaos.s22.a1o1.validation.exists.C22TermsOnlyExistsValidator;
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
@RosettaMeta(model=C22Terms.class)
public class C22TermsMeta implements RosettaMetaData<C22Terms> {

	@Override
	public List<Validator<? super C22Terms>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C22Terms, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C22Terms> validator(ValidatorFactory factory) {
		return factory.<C22Terms>create(C22TermsValidator.class);
	}

	@Override
	public Validator<? super C22Terms> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C22Terms>create(C22TermsTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C22Terms> validator() {
		return new C22TermsValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C22Terms> typeFormatValidator() {
		return new C22TermsTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C22Terms, Set<String>> onlyExistsValidator() {
		return new C22TermsOnlyExistsValidator();
	}
}
