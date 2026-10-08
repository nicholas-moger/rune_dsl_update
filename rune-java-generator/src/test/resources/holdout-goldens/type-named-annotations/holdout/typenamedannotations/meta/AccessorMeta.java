package holdout.typenamedannotations.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedannotations.Accessor;
import holdout.typenamedannotations.validation.AccessorTypeFormatValidator;
import holdout.typenamedannotations.validation.AccessorValidator;
import holdout.typenamedannotations.validation.exists.AccessorOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Accessor.class)
public class AccessorMeta implements RosettaMetaData<Accessor> {

	@Override
	public List<Validator<? super Accessor>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Accessor, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Accessor> validator(ValidatorFactory factory) {
		return factory.<Accessor>create(AccessorValidator.class);
	}

	@Override
	public Validator<? super Accessor> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Accessor>create(AccessorTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Accessor> validator() {
		return new AccessorValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Accessor> typeFormatValidator() {
		return new AccessorTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Accessor, Set<String>> onlyExistsValidator() {
		return new AccessorOnlyExistsValidator();
	}
}
