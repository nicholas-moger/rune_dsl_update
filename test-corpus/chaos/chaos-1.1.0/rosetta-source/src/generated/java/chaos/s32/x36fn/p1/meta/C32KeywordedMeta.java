package chaos.s32.x36fn.p1.meta;

import chaos.s32.x36fn.p1.C32Keyworded;
import chaos.s32.x36fn.p1.validation.C32KeywordedTypeFormatValidator;
import chaos.s32.x36fn.p1.validation.C32KeywordedValidator;
import chaos.s32.x36fn.p1.validation.exists.C32KeywordedOnlyExistsValidator;
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
@RosettaMeta(model=C32Keyworded.class)
public class C32KeywordedMeta implements RosettaMetaData<C32Keyworded> {

	@Override
	public List<Validator<? super C32Keyworded>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C32Keyworded, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C32Keyworded> validator(ValidatorFactory factory) {
		return factory.<C32Keyworded>create(C32KeywordedValidator.class);
	}

	@Override
	public Validator<? super C32Keyworded> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C32Keyworded>create(C32KeywordedTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C32Keyworded> validator() {
		return new C32KeywordedValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C32Keyworded> typeFormatValidator() {
		return new C32KeywordedTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C32Keyworded, Set<String>> onlyExistsValidator() {
		return new C32KeywordedOnlyExistsValidator();
	}
}
