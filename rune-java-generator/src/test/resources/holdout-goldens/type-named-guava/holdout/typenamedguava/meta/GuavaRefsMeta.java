package holdout.typenamedguava.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedguava.GuavaRefs;
import holdout.typenamedguava.validation.GuavaRefsTypeFormatValidator;
import holdout.typenamedguava.validation.GuavaRefsValidator;
import holdout.typenamedguava.validation.exists.GuavaRefsOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=GuavaRefs.class)
public class GuavaRefsMeta implements RosettaMetaData<GuavaRefs> {

	@Override
	public List<Validator<? super GuavaRefs>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super GuavaRefs, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super GuavaRefs> validator(ValidatorFactory factory) {
		return factory.<GuavaRefs>create(GuavaRefsValidator.class);
	}

	@Override
	public Validator<? super GuavaRefs> typeFormatValidator(ValidatorFactory factory) {
		return factory.<GuavaRefs>create(GuavaRefsTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super GuavaRefs> validator() {
		return new GuavaRefsValidator();
	}

	@Deprecated
	@Override
	public Validator<? super GuavaRefs> typeFormatValidator() {
		return new GuavaRefsTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super GuavaRefs, Set<String>> onlyExistsValidator() {
		return new GuavaRefsOnlyExistsValidator();
	}
}
