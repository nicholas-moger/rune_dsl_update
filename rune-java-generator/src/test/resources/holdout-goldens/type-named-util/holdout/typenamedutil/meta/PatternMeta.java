package holdout.typenamedutil.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedutil.Pattern;
import holdout.typenamedutil.validation.PatternTypeFormatValidator;
import holdout.typenamedutil.validation.PatternValidator;
import holdout.typenamedutil.validation.exists.PatternOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Pattern.class)
public class PatternMeta implements RosettaMetaData<Pattern> {

	@Override
	public List<Validator<? super Pattern>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Pattern, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Pattern> validator(ValidatorFactory factory) {
		return factory.<Pattern>create(PatternValidator.class);
	}

	@Override
	public Validator<? super Pattern> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Pattern>create(PatternTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Pattern> validator() {
		return new PatternValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Pattern> typeFormatValidator() {
		return new PatternTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Pattern, Set<String>> onlyExistsValidator() {
		return new PatternOnlyExistsValidator();
	}
}
