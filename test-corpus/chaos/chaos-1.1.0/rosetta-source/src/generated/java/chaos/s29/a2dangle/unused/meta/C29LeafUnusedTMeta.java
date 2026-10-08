package chaos.s29.a2dangle.unused.meta;

import chaos.s29.a2dangle.unused.C29LeafUnusedT;
import chaos.s29.a2dangle.unused.validation.C29LeafUnusedTTypeFormatValidator;
import chaos.s29.a2dangle.unused.validation.C29LeafUnusedTValidator;
import chaos.s29.a2dangle.unused.validation.exists.C29LeafUnusedTOnlyExistsValidator;
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
@RosettaMeta(model=C29LeafUnusedT.class)
public class C29LeafUnusedTMeta implements RosettaMetaData<C29LeafUnusedT> {

	@Override
	public List<Validator<? super C29LeafUnusedT>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C29LeafUnusedT, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C29LeafUnusedT> validator(ValidatorFactory factory) {
		return factory.<C29LeafUnusedT>create(C29LeafUnusedTValidator.class);
	}

	@Override
	public Validator<? super C29LeafUnusedT> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C29LeafUnusedT>create(C29LeafUnusedTTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C29LeafUnusedT> validator() {
		return new C29LeafUnusedTValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C29LeafUnusedT> typeFormatValidator() {
		return new C29LeafUnusedTTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C29LeafUnusedT, Set<String>> onlyExistsValidator() {
		return new C29LeafUnusedTOnlyExistsValidator();
	}
}
