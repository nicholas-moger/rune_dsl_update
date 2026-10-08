package chaos.s27.a2dangle.unused.meta;

import chaos.s27.a2dangle.unused.C27RefUnusedT;
import chaos.s27.a2dangle.unused.validation.C27RefUnusedTTypeFormatValidator;
import chaos.s27.a2dangle.unused.validation.C27RefUnusedTValidator;
import chaos.s27.a2dangle.unused.validation.exists.C27RefUnusedTOnlyExistsValidator;
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
@RosettaMeta(model=C27RefUnusedT.class)
public class C27RefUnusedTMeta implements RosettaMetaData<C27RefUnusedT> {

	@Override
	public List<Validator<? super C27RefUnusedT>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C27RefUnusedT, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C27RefUnusedT> validator(ValidatorFactory factory) {
		return factory.<C27RefUnusedT>create(C27RefUnusedTValidator.class);
	}

	@Override
	public Validator<? super C27RefUnusedT> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C27RefUnusedT>create(C27RefUnusedTTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C27RefUnusedT> validator() {
		return new C27RefUnusedTValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C27RefUnusedT> typeFormatValidator() {
		return new C27RefUnusedTTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C27RefUnusedT, Set<String>> onlyExistsValidator() {
		return new C27RefUnusedTOnlyExistsValidator();
	}
}
