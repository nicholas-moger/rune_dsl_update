package chaos.s24.a2dangle.unused.meta;

import chaos.s24.a2dangle.unused.C24RefUnusedT;
import chaos.s24.a2dangle.unused.validation.C24RefUnusedTTypeFormatValidator;
import chaos.s24.a2dangle.unused.validation.C24RefUnusedTValidator;
import chaos.s24.a2dangle.unused.validation.exists.C24RefUnusedTOnlyExistsValidator;
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
@RosettaMeta(model=C24RefUnusedT.class)
public class C24RefUnusedTMeta implements RosettaMetaData<C24RefUnusedT> {

	@Override
	public List<Validator<? super C24RefUnusedT>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C24RefUnusedT, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C24RefUnusedT> validator(ValidatorFactory factory) {
		return factory.<C24RefUnusedT>create(C24RefUnusedTValidator.class);
	}

	@Override
	public Validator<? super C24RefUnusedT> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C24RefUnusedT>create(C24RefUnusedTTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C24RefUnusedT> validator() {
		return new C24RefUnusedTValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C24RefUnusedT> typeFormatValidator() {
		return new C24RefUnusedTTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C24RefUnusedT, Set<String>> onlyExistsValidator() {
		return new C24RefUnusedTOnlyExistsValidator();
	}
}
