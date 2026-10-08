package chaos.s18.a2dangle.unused.meta;

import chaos.s18.a2dangle.unused.C18SubUnusedT;
import chaos.s18.a2dangle.unused.validation.C18SubUnusedTTypeFormatValidator;
import chaos.s18.a2dangle.unused.validation.C18SubUnusedTValidator;
import chaos.s18.a2dangle.unused.validation.exists.C18SubUnusedTOnlyExistsValidator;
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
@RosettaMeta(model=C18SubUnusedT.class)
public class C18SubUnusedTMeta implements RosettaMetaData<C18SubUnusedT> {

	@Override
	public List<Validator<? super C18SubUnusedT>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C18SubUnusedT, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C18SubUnusedT> validator(ValidatorFactory factory) {
		return factory.<C18SubUnusedT>create(C18SubUnusedTValidator.class);
	}

	@Override
	public Validator<? super C18SubUnusedT> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C18SubUnusedT>create(C18SubUnusedTTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C18SubUnusedT> validator() {
		return new C18SubUnusedTValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C18SubUnusedT> typeFormatValidator() {
		return new C18SubUnusedTTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C18SubUnusedT, Set<String>> onlyExistsValidator() {
		return new C18SubUnusedTOnlyExistsValidator();
	}
}
