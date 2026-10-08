package chaos.s29.a4snap.meta;

import chaos.s29.a4snap.C29Wrap;
import chaos.s29.a4snap.validation.C29WrapTypeFormatValidator;
import chaos.s29.a4snap.validation.C29WrapValidator;
import chaos.s29.a4snap.validation.exists.C29WrapOnlyExistsValidator;
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
 * @version 1.0.0-SNAPSHOT
 */
@RosettaMeta(model=C29Wrap.class)
public class C29WrapMeta implements RosettaMetaData<C29Wrap> {

	@Override
	public List<Validator<? super C29Wrap>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C29Wrap, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C29Wrap> validator(ValidatorFactory factory) {
		return factory.<C29Wrap>create(C29WrapValidator.class);
	}

	@Override
	public Validator<? super C29Wrap> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C29Wrap>create(C29WrapTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C29Wrap> validator() {
		return new C29WrapValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C29Wrap> typeFormatValidator() {
		return new C29WrapTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C29Wrap, Set<String>> onlyExistsValidator() {
		return new C29WrapOnlyExistsValidator();
	}
}
