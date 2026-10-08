package chaos.s23.a2dangle.unused.meta;

import chaos.s23.a2dangle.unused.C23BoxUnusedT;
import chaos.s23.a2dangle.unused.validation.C23BoxUnusedTTypeFormatValidator;
import chaos.s23.a2dangle.unused.validation.C23BoxUnusedTValidator;
import chaos.s23.a2dangle.unused.validation.exists.C23BoxUnusedTOnlyExistsValidator;
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
@RosettaMeta(model=C23BoxUnusedT.class)
public class C23BoxUnusedTMeta implements RosettaMetaData<C23BoxUnusedT> {

	@Override
	public List<Validator<? super C23BoxUnusedT>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C23BoxUnusedT, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C23BoxUnusedT> validator(ValidatorFactory factory) {
		return factory.<C23BoxUnusedT>create(C23BoxUnusedTValidator.class);
	}

	@Override
	public Validator<? super C23BoxUnusedT> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C23BoxUnusedT>create(C23BoxUnusedTTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C23BoxUnusedT> validator() {
		return new C23BoxUnusedTValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C23BoxUnusedT> typeFormatValidator() {
		return new C23BoxUnusedTTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C23BoxUnusedT, Set<String>> onlyExistsValidator() {
		return new C23BoxUnusedTOnlyExistsValidator();
	}
}
