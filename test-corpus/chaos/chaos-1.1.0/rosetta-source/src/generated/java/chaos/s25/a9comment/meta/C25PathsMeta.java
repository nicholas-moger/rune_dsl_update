package chaos.s25.a9comment.meta;

import chaos.s25.a9comment.C25Paths;
import chaos.s25.a9comment.validation.C25PathsTypeFormatValidator;
import chaos.s25.a9comment.validation.C25PathsValidator;
import chaos.s25.a9comment.validation.datarule.C25PathsC25Bare;
import chaos.s25.a9comment.validation.datarule.C25PathsC25Item;
import chaos.s25.a9comment.validation.datarule.C25PathsC25Option;
import chaos.s25.a9comment.validation.datarule.C25PathsC25TwoHop;
import chaos.s25.a9comment.validation.exists.C25PathsOnlyExistsValidator;
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
@RosettaMeta(model=C25Paths.class)
public class C25PathsMeta implements RosettaMetaData<C25Paths> {

	@Override
	public List<Validator<? super C25Paths>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
			factory.<C25Paths>create(C25PathsC25Bare.class),
			factory.<C25Paths>create(C25PathsC25Item.class),
			factory.<C25Paths>create(C25PathsC25TwoHop.class),
			factory.<C25Paths>create(C25PathsC25Option.class)
		);
	}
	
	@Override
	public List<Function<? super C25Paths, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C25Paths> validator(ValidatorFactory factory) {
		return factory.<C25Paths>create(C25PathsValidator.class);
	}

	@Override
	public Validator<? super C25Paths> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C25Paths>create(C25PathsTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C25Paths> validator() {
		return new C25PathsValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C25Paths> typeFormatValidator() {
		return new C25PathsTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C25Paths, Set<String>> onlyExistsValidator() {
		return new C25PathsOnlyExistsValidator();
	}
}
