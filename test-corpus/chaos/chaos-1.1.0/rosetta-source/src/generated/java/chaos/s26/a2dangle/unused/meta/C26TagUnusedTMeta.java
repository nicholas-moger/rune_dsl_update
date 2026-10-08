package chaos.s26.a2dangle.unused.meta;

import chaos.s26.a2dangle.unused.C26TagUnusedT;
import chaos.s26.a2dangle.unused.validation.C26TagUnusedTTypeFormatValidator;
import chaos.s26.a2dangle.unused.validation.C26TagUnusedTValidator;
import chaos.s26.a2dangle.unused.validation.exists.C26TagUnusedTOnlyExistsValidator;
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
@RosettaMeta(model=C26TagUnusedT.class)
public class C26TagUnusedTMeta implements RosettaMetaData<C26TagUnusedT> {

	@Override
	public List<Validator<? super C26TagUnusedT>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C26TagUnusedT, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C26TagUnusedT> validator(ValidatorFactory factory) {
		return factory.<C26TagUnusedT>create(C26TagUnusedTValidator.class);
	}

	@Override
	public Validator<? super C26TagUnusedT> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C26TagUnusedT>create(C26TagUnusedTTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C26TagUnusedT> validator() {
		return new C26TagUnusedTValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C26TagUnusedT> typeFormatValidator() {
		return new C26TagUnusedTTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C26TagUnusedT, Set<String>> onlyExistsValidator() {
		return new C26TagUnusedTOnlyExistsValidator();
	}
}
