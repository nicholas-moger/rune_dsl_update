package chaos.s08.a2dangle.unused.meta;

import chaos.s08.a2dangle.unused.C8BoxUnusedT;
import chaos.s08.a2dangle.unused.validation.C8BoxUnusedTTypeFormatValidator;
import chaos.s08.a2dangle.unused.validation.C8BoxUnusedTValidator;
import chaos.s08.a2dangle.unused.validation.exists.C8BoxUnusedTOnlyExistsValidator;
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
@RosettaMeta(model=C8BoxUnusedT.class)
public class C8BoxUnusedTMeta implements RosettaMetaData<C8BoxUnusedT> {

	@Override
	public List<Validator<? super C8BoxUnusedT>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C8BoxUnusedT, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C8BoxUnusedT> validator(ValidatorFactory factory) {
		return factory.<C8BoxUnusedT>create(C8BoxUnusedTValidator.class);
	}

	@Override
	public Validator<? super C8BoxUnusedT> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C8BoxUnusedT>create(C8BoxUnusedTTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C8BoxUnusedT> validator() {
		return new C8BoxUnusedTValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C8BoxUnusedT> typeFormatValidator() {
		return new C8BoxUnusedTTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C8BoxUnusedT, Set<String>> onlyExistsValidator() {
		return new C8BoxUnusedTOnlyExistsValidator();
	}
}
