package chaos.s26.a9comment.meta;

import chaos.s26.a9comment.C26Tag;
import chaos.s26.a9comment.validation.C26TagTypeFormatValidator;
import chaos.s26.a9comment.validation.C26TagValidator;
import chaos.s26.a9comment.validation.exists.C26TagOnlyExistsValidator;
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
@RosettaMeta(model=C26Tag.class)
public class C26TagMeta implements RosettaMetaData<C26Tag> {

	@Override
	public List<Validator<? super C26Tag>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C26Tag, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C26Tag> validator(ValidatorFactory factory) {
		return factory.<C26Tag>create(C26TagValidator.class);
	}

	@Override
	public Validator<? super C26Tag> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C26Tag>create(C26TagTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C26Tag> validator() {
		return new C26TagValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C26Tag> typeFormatValidator() {
		return new C26TagTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C26Tag, Set<String>> onlyExistsValidator() {
		return new C26TagOnlyExistsValidator();
	}
}
