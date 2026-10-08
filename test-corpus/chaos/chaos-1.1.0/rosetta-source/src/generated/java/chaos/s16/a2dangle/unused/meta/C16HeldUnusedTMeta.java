package chaos.s16.a2dangle.unused.meta;

import chaos.s16.a2dangle.unused.C16HeldUnusedT;
import chaos.s16.a2dangle.unused.validation.C16HeldUnusedTTypeFormatValidator;
import chaos.s16.a2dangle.unused.validation.C16HeldUnusedTValidator;
import chaos.s16.a2dangle.unused.validation.exists.C16HeldUnusedTOnlyExistsValidator;
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
@RosettaMeta(model=C16HeldUnusedT.class)
public class C16HeldUnusedTMeta implements RosettaMetaData<C16HeldUnusedT> {

	@Override
	public List<Validator<? super C16HeldUnusedT>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C16HeldUnusedT, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C16HeldUnusedT> validator(ValidatorFactory factory) {
		return factory.<C16HeldUnusedT>create(C16HeldUnusedTValidator.class);
	}

	@Override
	public Validator<? super C16HeldUnusedT> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C16HeldUnusedT>create(C16HeldUnusedTTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C16HeldUnusedT> validator() {
		return new C16HeldUnusedTValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C16HeldUnusedT> typeFormatValidator() {
		return new C16HeldUnusedTTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C16HeldUnusedT, Set<String>> onlyExistsValidator() {
		return new C16HeldUnusedTOnlyExistsValidator();
	}
}
