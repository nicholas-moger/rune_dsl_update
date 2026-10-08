package chaos.s14.a2dangle.unused.meta;

import chaos.s14.a2dangle.unused.C14AuxUnusedT;
import chaos.s14.a2dangle.unused.validation.C14AuxUnusedTTypeFormatValidator;
import chaos.s14.a2dangle.unused.validation.C14AuxUnusedTValidator;
import chaos.s14.a2dangle.unused.validation.exists.C14AuxUnusedTOnlyExistsValidator;
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
@RosettaMeta(model=C14AuxUnusedT.class)
public class C14AuxUnusedTMeta implements RosettaMetaData<C14AuxUnusedT> {

	@Override
	public List<Validator<? super C14AuxUnusedT>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C14AuxUnusedT, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C14AuxUnusedT> validator(ValidatorFactory factory) {
		return factory.<C14AuxUnusedT>create(C14AuxUnusedTValidator.class);
	}

	@Override
	public Validator<? super C14AuxUnusedT> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C14AuxUnusedT>create(C14AuxUnusedTTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C14AuxUnusedT> validator() {
		return new C14AuxUnusedTValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C14AuxUnusedT> typeFormatValidator() {
		return new C14AuxUnusedTTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C14AuxUnusedT, Set<String>> onlyExistsValidator() {
		return new C14AuxUnusedTOnlyExistsValidator();
	}
}
