package holdout.typenamedguava.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedguava.ImmutableMap;
import holdout.typenamedguava.validation.ImmutableMapTypeFormatValidator;
import holdout.typenamedguava.validation.ImmutableMapValidator;
import holdout.typenamedguava.validation.exists.ImmutableMapOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=ImmutableMap.class)
public class ImmutableMapMeta implements RosettaMetaData<ImmutableMap> {

	@Override
	public List<Validator<? super ImmutableMap>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super ImmutableMap, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super ImmutableMap> validator(ValidatorFactory factory) {
		return factory.<ImmutableMap>create(ImmutableMapValidator.class);
	}

	@Override
	public Validator<? super ImmutableMap> typeFormatValidator(ValidatorFactory factory) {
		return factory.<ImmutableMap>create(ImmutableMapTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super ImmutableMap> validator() {
		return new ImmutableMapValidator();
	}

	@Deprecated
	@Override
	public Validator<? super ImmutableMap> typeFormatValidator() {
		return new ImmutableMapTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super ImmutableMap, Set<String>> onlyExistsValidator() {
		return new ImmutableMapOnlyExistsValidator();
	}
}
