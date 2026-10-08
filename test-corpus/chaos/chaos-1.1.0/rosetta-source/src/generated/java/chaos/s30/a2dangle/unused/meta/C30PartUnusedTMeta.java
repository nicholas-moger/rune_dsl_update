package chaos.s30.a2dangle.unused.meta;

import chaos.s30.a2dangle.unused.C30PartUnusedT;
import chaos.s30.a2dangle.unused.validation.C30PartUnusedTTypeFormatValidator;
import chaos.s30.a2dangle.unused.validation.C30PartUnusedTValidator;
import chaos.s30.a2dangle.unused.validation.exists.C30PartUnusedTOnlyExistsValidator;
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
@RosettaMeta(model=C30PartUnusedT.class)
public class C30PartUnusedTMeta implements RosettaMetaData<C30PartUnusedT> {

	@Override
	public List<Validator<? super C30PartUnusedT>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C30PartUnusedT, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C30PartUnusedT> validator(ValidatorFactory factory) {
		return factory.<C30PartUnusedT>create(C30PartUnusedTValidator.class);
	}

	@Override
	public Validator<? super C30PartUnusedT> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C30PartUnusedT>create(C30PartUnusedTTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C30PartUnusedT> validator() {
		return new C30PartUnusedTValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C30PartUnusedT> typeFormatValidator() {
		return new C30PartUnusedTTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C30PartUnusedT, Set<String>> onlyExistsValidator() {
		return new C30PartUnusedTOnlyExistsValidator();
	}
}
