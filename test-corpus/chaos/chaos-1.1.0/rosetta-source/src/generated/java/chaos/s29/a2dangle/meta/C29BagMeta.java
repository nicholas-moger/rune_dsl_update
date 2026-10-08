package chaos.s29.a2dangle.meta;

import chaos.s29.a2dangle.C29Bag;
import chaos.s29.a2dangle.validation.C29BagTypeFormatValidator;
import chaos.s29.a2dangle.validation.C29BagValidator;
import chaos.s29.a2dangle.validation.datarule.C29BagC29DeepCond;
import chaos.s29.a2dangle.validation.exists.C29BagOnlyExistsValidator;
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
@RosettaMeta(model=C29Bag.class)
public class C29BagMeta implements RosettaMetaData<C29Bag> {

	@Override
	public List<Validator<? super C29Bag>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
			factory.<C29Bag>create(C29BagC29DeepCond.class)
		);
	}
	
	@Override
	public List<Function<? super C29Bag, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C29Bag> validator(ValidatorFactory factory) {
		return factory.<C29Bag>create(C29BagValidator.class);
	}

	@Override
	public Validator<? super C29Bag> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C29Bag>create(C29BagTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C29Bag> validator() {
		return new C29BagValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C29Bag> typeFormatValidator() {
		return new C29BagTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C29Bag, Set<String>> onlyExistsValidator() {
		return new C29BagOnlyExistsValidator();
	}
}
