package chaos.s06.a2dangle.unused.meta;

import chaos.s06.a2dangle.unused.C6TagUnusedT;
import chaos.s06.a2dangle.unused.validation.C6TagUnusedTTypeFormatValidator;
import chaos.s06.a2dangle.unused.validation.C6TagUnusedTValidator;
import chaos.s06.a2dangle.unused.validation.exists.C6TagUnusedTOnlyExistsValidator;
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
@RosettaMeta(model=C6TagUnusedT.class)
public class C6TagUnusedTMeta implements RosettaMetaData<C6TagUnusedT> {

	@Override
	public List<Validator<? super C6TagUnusedT>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C6TagUnusedT, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C6TagUnusedT> validator(ValidatorFactory factory) {
		return factory.<C6TagUnusedT>create(C6TagUnusedTValidator.class);
	}

	@Override
	public Validator<? super C6TagUnusedT> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C6TagUnusedT>create(C6TagUnusedTTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C6TagUnusedT> validator() {
		return new C6TagUnusedTValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C6TagUnusedT> typeFormatValidator() {
		return new C6TagUnusedTTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C6TagUnusedT, Set<String>> onlyExistsValidator() {
		return new C6TagUnusedTOnlyExistsValidator();
	}
}
