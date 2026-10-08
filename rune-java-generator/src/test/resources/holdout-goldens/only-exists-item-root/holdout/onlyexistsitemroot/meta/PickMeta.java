package holdout.onlyexistsitemroot.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.onlyexistsitemroot.Pick;
import holdout.onlyexistsitemroot.validation.PickTypeFormatValidator;
import holdout.onlyexistsitemroot.validation.PickValidator;
import holdout.onlyexistsitemroot.validation.datarule.PickChoice;
import holdout.onlyexistsitemroot.validation.exists.PickOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Pick.class)
public class PickMeta implements RosettaMetaData<Pick> {

	@Override
	public List<Validator<? super Pick>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
			factory.<Pick>create(PickChoice.class)
		);
	}
	
	@Override
	public List<Function<? super Pick, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Pick> validator(ValidatorFactory factory) {
		return factory.<Pick>create(PickValidator.class);
	}

	@Override
	public Validator<? super Pick> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Pick>create(PickTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Pick> validator() {
		return new PickValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Pick> typeFormatValidator() {
		return new PickTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Pick, Set<String>> onlyExistsValidator() {
		return new PickOnlyExistsValidator();
	}
}
