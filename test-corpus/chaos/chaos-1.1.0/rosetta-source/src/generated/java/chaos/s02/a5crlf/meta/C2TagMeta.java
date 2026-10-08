package chaos.s02.a5crlf.meta;

import chaos.s02.a5crlf.C2Tag;
import chaos.s02.a5crlf.validation.C2TagTypeFormatValidator;
import chaos.s02.a5crlf.validation.C2TagValidator;
import chaos.s02.a5crlf.validation.exists.C2TagOnlyExistsValidator;
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
@RosettaMeta(model=C2Tag.class)
public class C2TagMeta implements RosettaMetaData<C2Tag> {

	@Override
	public List<Validator<? super C2Tag>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C2Tag, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C2Tag> validator(ValidatorFactory factory) {
		return factory.<C2Tag>create(C2TagValidator.class);
	}

	@Override
	public Validator<? super C2Tag> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C2Tag>create(C2TagTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C2Tag> validator() {
		return new C2TagValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C2Tag> typeFormatValidator() {
		return new C2TagTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C2Tag, Set<String>> onlyExistsValidator() {
		return new C2TagOnlyExistsValidator();
	}
}
