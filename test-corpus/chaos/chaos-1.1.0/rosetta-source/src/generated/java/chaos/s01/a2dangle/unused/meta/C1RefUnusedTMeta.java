package chaos.s01.a2dangle.unused.meta;

import chaos.s01.a2dangle.unused.C1RefUnusedT;
import chaos.s01.a2dangle.unused.validation.C1RefUnusedTTypeFormatValidator;
import chaos.s01.a2dangle.unused.validation.C1RefUnusedTValidator;
import chaos.s01.a2dangle.unused.validation.exists.C1RefUnusedTOnlyExistsValidator;
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
@RosettaMeta(model=C1RefUnusedT.class)
public class C1RefUnusedTMeta implements RosettaMetaData<C1RefUnusedT> {

	@Override
	public List<Validator<? super C1RefUnusedT>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C1RefUnusedT, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C1RefUnusedT> validator(ValidatorFactory factory) {
		return factory.<C1RefUnusedT>create(C1RefUnusedTValidator.class);
	}

	@Override
	public Validator<? super C1RefUnusedT> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C1RefUnusedT>create(C1RefUnusedTTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C1RefUnusedT> validator() {
		return new C1RefUnusedTValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C1RefUnusedT> typeFormatValidator() {
		return new C1RefUnusedTTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C1RefUnusedT, Set<String>> onlyExistsValidator() {
		return new C1RefUnusedTOnlyExistsValidator();
	}
}
