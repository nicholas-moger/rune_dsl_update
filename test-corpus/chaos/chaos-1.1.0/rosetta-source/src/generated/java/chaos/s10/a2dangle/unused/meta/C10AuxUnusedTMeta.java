package chaos.s10.a2dangle.unused.meta;

import chaos.s10.a2dangle.unused.C10AuxUnusedT;
import chaos.s10.a2dangle.unused.validation.C10AuxUnusedTTypeFormatValidator;
import chaos.s10.a2dangle.unused.validation.C10AuxUnusedTValidator;
import chaos.s10.a2dangle.unused.validation.exists.C10AuxUnusedTOnlyExistsValidator;
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
@RosettaMeta(model=C10AuxUnusedT.class)
public class C10AuxUnusedTMeta implements RosettaMetaData<C10AuxUnusedT> {

	@Override
	public List<Validator<? super C10AuxUnusedT>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C10AuxUnusedT, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C10AuxUnusedT> validator(ValidatorFactory factory) {
		return factory.<C10AuxUnusedT>create(C10AuxUnusedTValidator.class);
	}

	@Override
	public Validator<? super C10AuxUnusedT> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C10AuxUnusedT>create(C10AuxUnusedTTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C10AuxUnusedT> validator() {
		return new C10AuxUnusedTValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C10AuxUnusedT> typeFormatValidator() {
		return new C10AuxUnusedTTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C10AuxUnusedT, Set<String>> onlyExistsValidator() {
		return new C10AuxUnusedTOnlyExistsValidator();
	}
}
