package holdout.typenamedutil.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedutil.Consumer;
import holdout.typenamedutil.validation.ConsumerTypeFormatValidator;
import holdout.typenamedutil.validation.ConsumerValidator;
import holdout.typenamedutil.validation.exists.ConsumerOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Consumer.class)
public class ConsumerMeta implements RosettaMetaData<Consumer> {

	@Override
	public List<Validator<? super Consumer>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Consumer, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Consumer> validator(ValidatorFactory factory) {
		return factory.<Consumer>create(ConsumerValidator.class);
	}

	@Override
	public Validator<? super Consumer> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Consumer>create(ConsumerTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Consumer> validator() {
		return new ConsumerValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Consumer> typeFormatValidator() {
		return new ConsumerTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Consumer, Set<String>> onlyExistsValidator() {
		return new ConsumerOnlyExistsValidator();
	}
}
