package holdout.typenamedannotations.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedannotations.validation.RosettaMetaTypeFormatValidator;
import holdout.typenamedannotations.validation.RosettaMetaValidator;
import holdout.typenamedannotations.validation.exists.RosettaMetaOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=holdout.typenamedannotations.RosettaMeta.class)
public class RosettaMetaMeta implements RosettaMetaData<holdout.typenamedannotations.RosettaMeta> {

	@Override
	public List<Validator<? super holdout.typenamedannotations.RosettaMeta>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super holdout.typenamedannotations.RosettaMeta, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super holdout.typenamedannotations.RosettaMeta> validator(ValidatorFactory factory) {
		return factory.<holdout.typenamedannotations.RosettaMeta>create(RosettaMetaValidator.class);
	}

	@Override
	public Validator<? super holdout.typenamedannotations.RosettaMeta> typeFormatValidator(ValidatorFactory factory) {
		return factory.<holdout.typenamedannotations.RosettaMeta>create(RosettaMetaTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super holdout.typenamedannotations.RosettaMeta> validator() {
		return new RosettaMetaValidator();
	}

	@Deprecated
	@Override
	public Validator<? super holdout.typenamedannotations.RosettaMeta> typeFormatValidator() {
		return new RosettaMetaTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super holdout.typenamedannotations.RosettaMeta, Set<String>> onlyExistsValidator() {
		return new RosettaMetaOnlyExistsValidator();
	}
}
