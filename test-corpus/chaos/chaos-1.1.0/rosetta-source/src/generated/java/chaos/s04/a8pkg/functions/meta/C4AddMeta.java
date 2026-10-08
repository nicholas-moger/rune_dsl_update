package chaos.s04.a8pkg.functions.meta;

import chaos.s04.a8pkg.functions.C4Add;
import chaos.s04.a8pkg.functions.validation.C4AddTypeFormatValidator;
import chaos.s04.a8pkg.functions.validation.C4AddValidator;
import chaos.s04.a8pkg.functions.validation.exists.C4AddOnlyExistsValidator;
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
@RosettaMeta(model=C4Add.class)
public class C4AddMeta implements RosettaMetaData<C4Add> {

	@Override
	public List<Validator<? super C4Add>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C4Add, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C4Add> validator(ValidatorFactory factory) {
		return factory.<C4Add>create(C4AddValidator.class);
	}

	@Override
	public Validator<? super C4Add> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C4Add>create(C4AddTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C4Add> validator() {
		return new C4AddValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C4Add> typeFormatValidator() {
		return new C4AddTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C4Add, Set<String>> onlyExistsValidator() {
		return new C4AddOnlyExistsValidator();
	}
}
