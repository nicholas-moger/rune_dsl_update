package holdout.onlyexistsitemroot.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.onlyexistsitemroot.Paths;
import holdout.onlyexistsitemroot.validation.PathsTypeFormatValidator;
import holdout.onlyexistsitemroot.validation.PathsValidator;
import holdout.onlyexistsitemroot.validation.datarule.PathsBareControl;
import holdout.onlyexistsitemroot.validation.datarule.PathsItemOption;
import holdout.onlyexistsitemroot.validation.datarule.PathsItemRoot;
import holdout.onlyexistsitemroot.validation.datarule.PathsItemTwoHop;
import holdout.onlyexistsitemroot.validation.exists.PathsOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Paths.class)
public class PathsMeta implements RosettaMetaData<Paths> {

	@Override
	public List<Validator<? super Paths>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
			factory.<Paths>create(PathsItemRoot.class),
			factory.<Paths>create(PathsItemTwoHop.class),
			factory.<Paths>create(PathsItemOption.class),
			factory.<Paths>create(PathsBareControl.class)
		);
	}
	
	@Override
	public List<Function<? super Paths, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Paths> validator(ValidatorFactory factory) {
		return factory.<Paths>create(PathsValidator.class);
	}

	@Override
	public Validator<? super Paths> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Paths>create(PathsTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Paths> validator() {
		return new PathsValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Paths> typeFormatValidator() {
		return new PathsTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Paths, Set<String>> onlyExistsValidator() {
		return new PathsOnlyExistsValidator();
	}
}
