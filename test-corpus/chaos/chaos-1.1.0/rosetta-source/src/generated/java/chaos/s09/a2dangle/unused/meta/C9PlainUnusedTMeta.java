package chaos.s09.a2dangle.unused.meta;

import chaos.s09.a2dangle.unused.C9PlainUnusedT;
import chaos.s09.a2dangle.unused.validation.C9PlainUnusedTTypeFormatValidator;
import chaos.s09.a2dangle.unused.validation.C9PlainUnusedTValidator;
import chaos.s09.a2dangle.unused.validation.exists.C9PlainUnusedTOnlyExistsValidator;
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
@RosettaMeta(model=C9PlainUnusedT.class)
public class C9PlainUnusedTMeta implements RosettaMetaData<C9PlainUnusedT> {

	@Override
	public List<Validator<? super C9PlainUnusedT>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C9PlainUnusedT, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C9PlainUnusedT> validator(ValidatorFactory factory) {
		return factory.<C9PlainUnusedT>create(C9PlainUnusedTValidator.class);
	}

	@Override
	public Validator<? super C9PlainUnusedT> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C9PlainUnusedT>create(C9PlainUnusedTTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C9PlainUnusedT> validator() {
		return new C9PlainUnusedTValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C9PlainUnusedT> typeFormatValidator() {
		return new C9PlainUnusedTTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C9PlainUnusedT, Set<String>> onlyExistsValidator() {
		return new C9PlainUnusedTOnlyExistsValidator();
	}
}
