package chaos.s34.a2dangle.unused.meta;

import chaos.s34.a2dangle.unused.C34AuxUnusedT;
import chaos.s34.a2dangle.unused.validation.C34AuxUnusedTTypeFormatValidator;
import chaos.s34.a2dangle.unused.validation.C34AuxUnusedTValidator;
import chaos.s34.a2dangle.unused.validation.exists.C34AuxUnusedTOnlyExistsValidator;
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
@RosettaMeta(model=C34AuxUnusedT.class)
public class C34AuxUnusedTMeta implements RosettaMetaData<C34AuxUnusedT> {

	@Override
	public List<Validator<? super C34AuxUnusedT>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C34AuxUnusedT, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C34AuxUnusedT> validator(ValidatorFactory factory) {
		return factory.<C34AuxUnusedT>create(C34AuxUnusedTValidator.class);
	}

	@Override
	public Validator<? super C34AuxUnusedT> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C34AuxUnusedT>create(C34AuxUnusedTTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C34AuxUnusedT> validator() {
		return new C34AuxUnusedTValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C34AuxUnusedT> typeFormatValidator() {
		return new C34AuxUnusedTTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C34AuxUnusedT, Set<String>> onlyExistsValidator() {
		return new C34AuxUnusedTOnlyExistsValidator();
	}
}
