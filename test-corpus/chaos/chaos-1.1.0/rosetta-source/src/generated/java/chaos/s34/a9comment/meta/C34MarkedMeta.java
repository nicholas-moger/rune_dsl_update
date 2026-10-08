package chaos.s34.a9comment.meta;

import chaos.s34.a9comment.C34Marked;
import chaos.s34.a9comment.validation.C34MarkedTypeFormatValidator;
import chaos.s34.a9comment.validation.C34MarkedValidator;
import chaos.s34.a9comment.validation.exists.C34MarkedOnlyExistsValidator;
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
@RosettaMeta(model=C34Marked.class)
public class C34MarkedMeta implements RosettaMetaData<C34Marked> {

	@Override
	public List<Validator<? super C34Marked>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C34Marked, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C34Marked> validator(ValidatorFactory factory) {
		return factory.<C34Marked>create(C34MarkedValidator.class);
	}

	@Override
	public Validator<? super C34Marked> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C34Marked>create(C34MarkedTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C34Marked> validator() {
		return new C34MarkedValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C34Marked> typeFormatValidator() {
		return new C34MarkedTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C34Marked, Set<String>> onlyExistsValidator() {
		return new C34MarkedOnlyExistsValidator();
	}
}
