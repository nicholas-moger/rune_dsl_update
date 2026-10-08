package chaos.s06.a1o4.meta;

import chaos.s06.a1o4.C6Tag;
import chaos.s06.a1o4.validation.C6TagTypeFormatValidator;
import chaos.s06.a1o4.validation.C6TagValidator;
import chaos.s06.a1o4.validation.exists.C6TagOnlyExistsValidator;
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
@RosettaMeta(model=C6Tag.class)
public class C6TagMeta implements RosettaMetaData<C6Tag> {

	@Override
	public List<Validator<? super C6Tag>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C6Tag, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C6Tag> validator(ValidatorFactory factory) {
		return factory.<C6Tag>create(C6TagValidator.class);
	}

	@Override
	public Validator<? super C6Tag> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C6Tag>create(C6TagTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C6Tag> validator() {
		return new C6TagValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C6Tag> typeFormatValidator() {
		return new C6TagTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C6Tag, Set<String>> onlyExistsValidator() {
		return new C6TagOnlyExistsValidator();
	}
}
