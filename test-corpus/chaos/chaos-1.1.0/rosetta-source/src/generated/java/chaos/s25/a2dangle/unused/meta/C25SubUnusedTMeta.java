package chaos.s25.a2dangle.unused.meta;

import chaos.s25.a2dangle.unused.C25SubUnusedT;
import chaos.s25.a2dangle.unused.validation.C25SubUnusedTTypeFormatValidator;
import chaos.s25.a2dangle.unused.validation.C25SubUnusedTValidator;
import chaos.s25.a2dangle.unused.validation.exists.C25SubUnusedTOnlyExistsValidator;
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
@RosettaMeta(model=C25SubUnusedT.class)
public class C25SubUnusedTMeta implements RosettaMetaData<C25SubUnusedT> {

	@Override
	public List<Validator<? super C25SubUnusedT>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C25SubUnusedT, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C25SubUnusedT> validator(ValidatorFactory factory) {
		return factory.<C25SubUnusedT>create(C25SubUnusedTValidator.class);
	}

	@Override
	public Validator<? super C25SubUnusedT> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C25SubUnusedT>create(C25SubUnusedTTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C25SubUnusedT> validator() {
		return new C25SubUnusedTValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C25SubUnusedT> typeFormatValidator() {
		return new C25SubUnusedTTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C25SubUnusedT, Set<String>> onlyExistsValidator() {
		return new C25SubUnusedTOnlyExistsValidator();
	}
}
