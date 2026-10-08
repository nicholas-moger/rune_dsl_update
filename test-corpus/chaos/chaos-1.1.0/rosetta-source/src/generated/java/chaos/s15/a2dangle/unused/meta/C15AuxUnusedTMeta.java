package chaos.s15.a2dangle.unused.meta;

import chaos.s15.a2dangle.unused.C15AuxUnusedT;
import chaos.s15.a2dangle.unused.validation.C15AuxUnusedTTypeFormatValidator;
import chaos.s15.a2dangle.unused.validation.C15AuxUnusedTValidator;
import chaos.s15.a2dangle.unused.validation.exists.C15AuxUnusedTOnlyExistsValidator;
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
@RosettaMeta(model=C15AuxUnusedT.class)
public class C15AuxUnusedTMeta implements RosettaMetaData<C15AuxUnusedT> {

	@Override
	public List<Validator<? super C15AuxUnusedT>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C15AuxUnusedT, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C15AuxUnusedT> validator(ValidatorFactory factory) {
		return factory.<C15AuxUnusedT>create(C15AuxUnusedTValidator.class);
	}

	@Override
	public Validator<? super C15AuxUnusedT> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C15AuxUnusedT>create(C15AuxUnusedTTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C15AuxUnusedT> validator() {
		return new C15AuxUnusedTValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C15AuxUnusedT> typeFormatValidator() {
		return new C15AuxUnusedTTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C15AuxUnusedT, Set<String>> onlyExistsValidator() {
		return new C15AuxUnusedTOnlyExistsValidator();
	}
}
