package chaos.s30.a9comment.meta;

import chaos.s30.a9comment.C30Part;
import chaos.s30.a9comment.validation.C30PartTypeFormatValidator;
import chaos.s30.a9comment.validation.C30PartValidator;
import chaos.s30.a9comment.validation.exists.C30PartOnlyExistsValidator;
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
@RosettaMeta(model=C30Part.class)
public class C30PartMeta implements RosettaMetaData<C30Part> {

	@Override
	public List<Validator<? super C30Part>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C30Part, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C30Part> validator(ValidatorFactory factory) {
		return factory.<C30Part>create(C30PartValidator.class);
	}

	@Override
	public Validator<? super C30Part> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C30Part>create(C30PartTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C30Part> validator() {
		return new C30PartValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C30Part> typeFormatValidator() {
		return new C30PartTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C30Part, Set<String>> onlyExistsValidator() {
		return new C30PartOnlyExistsValidator();
	}
}
