package chaos.s32.a2dangle.unused.meta;

import chaos.s32.a2dangle.unused.C32AuxUnusedT;
import chaos.s32.a2dangle.unused.validation.C32AuxUnusedTTypeFormatValidator;
import chaos.s32.a2dangle.unused.validation.C32AuxUnusedTValidator;
import chaos.s32.a2dangle.unused.validation.exists.C32AuxUnusedTOnlyExistsValidator;
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
@RosettaMeta(model=C32AuxUnusedT.class)
public class C32AuxUnusedTMeta implements RosettaMetaData<C32AuxUnusedT> {

	@Override
	public List<Validator<? super C32AuxUnusedT>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C32AuxUnusedT, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C32AuxUnusedT> validator(ValidatorFactory factory) {
		return factory.<C32AuxUnusedT>create(C32AuxUnusedTValidator.class);
	}

	@Override
	public Validator<? super C32AuxUnusedT> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C32AuxUnusedT>create(C32AuxUnusedTTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C32AuxUnusedT> validator() {
		return new C32AuxUnusedTValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C32AuxUnusedT> typeFormatValidator() {
		return new C32AuxUnusedTTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C32AuxUnusedT, Set<String>> onlyExistsValidator() {
		return new C32AuxUnusedTOnlyExistsValidator();
	}
}
