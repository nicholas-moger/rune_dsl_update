package chaos.s20.a2dangle.unused.meta;

import chaos.s20.a2dangle.unused.C20LeafUnusedT;
import chaos.s20.a2dangle.unused.validation.C20LeafUnusedTTypeFormatValidator;
import chaos.s20.a2dangle.unused.validation.C20LeafUnusedTValidator;
import chaos.s20.a2dangle.unused.validation.exists.C20LeafUnusedTOnlyExistsValidator;
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
@RosettaMeta(model=C20LeafUnusedT.class)
public class C20LeafUnusedTMeta implements RosettaMetaData<C20LeafUnusedT> {

	@Override
	public List<Validator<? super C20LeafUnusedT>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C20LeafUnusedT, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C20LeafUnusedT> validator(ValidatorFactory factory) {
		return factory.<C20LeafUnusedT>create(C20LeafUnusedTValidator.class);
	}

	@Override
	public Validator<? super C20LeafUnusedT> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C20LeafUnusedT>create(C20LeafUnusedTTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C20LeafUnusedT> validator() {
		return new C20LeafUnusedTValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C20LeafUnusedT> typeFormatValidator() {
		return new C20LeafUnusedTTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C20LeafUnusedT, Set<String>> onlyExistsValidator() {
		return new C20LeafUnusedTOnlyExistsValidator();
	}
}
