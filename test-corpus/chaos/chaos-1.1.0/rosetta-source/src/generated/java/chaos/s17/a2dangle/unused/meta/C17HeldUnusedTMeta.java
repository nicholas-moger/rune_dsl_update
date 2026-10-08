package chaos.s17.a2dangle.unused.meta;

import chaos.s17.a2dangle.unused.C17HeldUnusedT;
import chaos.s17.a2dangle.unused.validation.C17HeldUnusedTTypeFormatValidator;
import chaos.s17.a2dangle.unused.validation.C17HeldUnusedTValidator;
import chaos.s17.a2dangle.unused.validation.exists.C17HeldUnusedTOnlyExistsValidator;
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
@RosettaMeta(model=C17HeldUnusedT.class)
public class C17HeldUnusedTMeta implements RosettaMetaData<C17HeldUnusedT> {

	@Override
	public List<Validator<? super C17HeldUnusedT>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C17HeldUnusedT, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C17HeldUnusedT> validator(ValidatorFactory factory) {
		return factory.<C17HeldUnusedT>create(C17HeldUnusedTValidator.class);
	}

	@Override
	public Validator<? super C17HeldUnusedT> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C17HeldUnusedT>create(C17HeldUnusedTTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C17HeldUnusedT> validator() {
		return new C17HeldUnusedTValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C17HeldUnusedT> typeFormatValidator() {
		return new C17HeldUnusedTTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C17HeldUnusedT, Set<String>> onlyExistsValidator() {
		return new C17HeldUnusedTOnlyExistsValidator();
	}
}
