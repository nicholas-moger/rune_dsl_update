package chaos.s12.a2dangle.unused.meta;

import chaos.s12.a2dangle.unused.C12AuxUnusedT;
import chaos.s12.a2dangle.unused.validation.C12AuxUnusedTTypeFormatValidator;
import chaos.s12.a2dangle.unused.validation.C12AuxUnusedTValidator;
import chaos.s12.a2dangle.unused.validation.exists.C12AuxUnusedTOnlyExistsValidator;
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
@RosettaMeta(model=C12AuxUnusedT.class)
public class C12AuxUnusedTMeta implements RosettaMetaData<C12AuxUnusedT> {

	@Override
	public List<Validator<? super C12AuxUnusedT>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C12AuxUnusedT, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C12AuxUnusedT> validator(ValidatorFactory factory) {
		return factory.<C12AuxUnusedT>create(C12AuxUnusedTValidator.class);
	}

	@Override
	public Validator<? super C12AuxUnusedT> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C12AuxUnusedT>create(C12AuxUnusedTTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C12AuxUnusedT> validator() {
		return new C12AuxUnusedTValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C12AuxUnusedT> typeFormatValidator() {
		return new C12AuxUnusedTTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C12AuxUnusedT, Set<String>> onlyExistsValidator() {
		return new C12AuxUnusedTOnlyExistsValidator();
	}
}
