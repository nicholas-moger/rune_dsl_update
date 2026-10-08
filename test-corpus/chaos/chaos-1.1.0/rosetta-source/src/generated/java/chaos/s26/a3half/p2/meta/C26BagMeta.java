package chaos.s26.a3half.p2.meta;

import chaos.s26.a3half.p2.C26Bag;
import chaos.s26.a3half.p2.validation.C26BagTypeFormatValidator;
import chaos.s26.a3half.p2.validation.C26BagValidator;
import chaos.s26.a3half.p2.validation.datarule.C26BagC26Colour;
import chaos.s26.a3half.p2.validation.exists.C26BagOnlyExistsValidator;
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
@RosettaMeta(model=C26Bag.class)
public class C26BagMeta implements RosettaMetaData<C26Bag> {

	@Override
	public List<Validator<? super C26Bag>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
			factory.<C26Bag>create(C26BagC26Colour.class)
		);
	}
	
	@Override
	public List<Function<? super C26Bag, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C26Bag> validator(ValidatorFactory factory) {
		return factory.<C26Bag>create(C26BagValidator.class);
	}

	@Override
	public Validator<? super C26Bag> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C26Bag>create(C26BagTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C26Bag> validator() {
		return new C26BagValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C26Bag> typeFormatValidator() {
		return new C26BagTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C26Bag, Set<String>> onlyExistsValidator() {
		return new C26BagOnlyExistsValidator();
	}
}
