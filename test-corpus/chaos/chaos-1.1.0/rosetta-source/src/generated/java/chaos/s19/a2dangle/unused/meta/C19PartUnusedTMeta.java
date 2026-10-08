package chaos.s19.a2dangle.unused.meta;

import chaos.s19.a2dangle.unused.C19PartUnusedT;
import chaos.s19.a2dangle.unused.validation.C19PartUnusedTTypeFormatValidator;
import chaos.s19.a2dangle.unused.validation.C19PartUnusedTValidator;
import chaos.s19.a2dangle.unused.validation.exists.C19PartUnusedTOnlyExistsValidator;
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
@RosettaMeta(model=C19PartUnusedT.class)
public class C19PartUnusedTMeta implements RosettaMetaData<C19PartUnusedT> {

	@Override
	public List<Validator<? super C19PartUnusedT>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C19PartUnusedT, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C19PartUnusedT> validator(ValidatorFactory factory) {
		return factory.<C19PartUnusedT>create(C19PartUnusedTValidator.class);
	}

	@Override
	public Validator<? super C19PartUnusedT> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C19PartUnusedT>create(C19PartUnusedTTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C19PartUnusedT> validator() {
		return new C19PartUnusedTValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C19PartUnusedT> typeFormatValidator() {
		return new C19PartUnusedTTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C19PartUnusedT, Set<String>> onlyExistsValidator() {
		return new C19PartUnusedTOnlyExistsValidator();
	}
}
