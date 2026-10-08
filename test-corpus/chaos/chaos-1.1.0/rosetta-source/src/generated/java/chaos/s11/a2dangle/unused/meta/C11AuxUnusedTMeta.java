package chaos.s11.a2dangle.unused.meta;

import chaos.s11.a2dangle.unused.C11AuxUnusedT;
import chaos.s11.a2dangle.unused.validation.C11AuxUnusedTTypeFormatValidator;
import chaos.s11.a2dangle.unused.validation.C11AuxUnusedTValidator;
import chaos.s11.a2dangle.unused.validation.exists.C11AuxUnusedTOnlyExistsValidator;
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
@RosettaMeta(model=C11AuxUnusedT.class)
public class C11AuxUnusedTMeta implements RosettaMetaData<C11AuxUnusedT> {

	@Override
	public List<Validator<? super C11AuxUnusedT>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C11AuxUnusedT, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C11AuxUnusedT> validator(ValidatorFactory factory) {
		return factory.<C11AuxUnusedT>create(C11AuxUnusedTValidator.class);
	}

	@Override
	public Validator<? super C11AuxUnusedT> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C11AuxUnusedT>create(C11AuxUnusedTTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C11AuxUnusedT> validator() {
		return new C11AuxUnusedTValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C11AuxUnusedT> typeFormatValidator() {
		return new C11AuxUnusedTTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C11AuxUnusedT, Set<String>> onlyExistsValidator() {
		return new C11AuxUnusedTOnlyExistsValidator();
	}
}
