package chaos.s05.a2dangle.unused.meta;

import chaos.s05.a2dangle.unused.C5SubUnusedT;
import chaos.s05.a2dangle.unused.validation.C5SubUnusedTTypeFormatValidator;
import chaos.s05.a2dangle.unused.validation.C5SubUnusedTValidator;
import chaos.s05.a2dangle.unused.validation.exists.C5SubUnusedTOnlyExistsValidator;
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
@RosettaMeta(model=C5SubUnusedT.class)
public class C5SubUnusedTMeta implements RosettaMetaData<C5SubUnusedT> {

	@Override
	public List<Validator<? super C5SubUnusedT>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C5SubUnusedT, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C5SubUnusedT> validator(ValidatorFactory factory) {
		return factory.<C5SubUnusedT>create(C5SubUnusedTValidator.class);
	}

	@Override
	public Validator<? super C5SubUnusedT> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C5SubUnusedT>create(C5SubUnusedTTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C5SubUnusedT> validator() {
		return new C5SubUnusedTValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C5SubUnusedT> typeFormatValidator() {
		return new C5SubUnusedTTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C5SubUnusedT, Set<String>> onlyExistsValidator() {
		return new C5SubUnusedTOnlyExistsValidator();
	}
}
