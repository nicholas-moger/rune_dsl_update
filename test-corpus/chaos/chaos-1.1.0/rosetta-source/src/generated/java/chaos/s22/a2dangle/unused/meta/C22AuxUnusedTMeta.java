package chaos.s22.a2dangle.unused.meta;

import chaos.s22.a2dangle.unused.C22AuxUnusedT;
import chaos.s22.a2dangle.unused.validation.C22AuxUnusedTTypeFormatValidator;
import chaos.s22.a2dangle.unused.validation.C22AuxUnusedTValidator;
import chaos.s22.a2dangle.unused.validation.exists.C22AuxUnusedTOnlyExistsValidator;
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
@RosettaMeta(model=C22AuxUnusedT.class)
public class C22AuxUnusedTMeta implements RosettaMetaData<C22AuxUnusedT> {

	@Override
	public List<Validator<? super C22AuxUnusedT>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C22AuxUnusedT, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C22AuxUnusedT> validator(ValidatorFactory factory) {
		return factory.<C22AuxUnusedT>create(C22AuxUnusedTValidator.class);
	}

	@Override
	public Validator<? super C22AuxUnusedT> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C22AuxUnusedT>create(C22AuxUnusedTTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C22AuxUnusedT> validator() {
		return new C22AuxUnusedTValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C22AuxUnusedT> typeFormatValidator() {
		return new C22AuxUnusedTTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C22AuxUnusedT, Set<String>> onlyExistsValidator() {
		return new C22AuxUnusedTOnlyExistsValidator();
	}
}
