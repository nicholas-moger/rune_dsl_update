package chaos.s13.a2wild.meta;

import chaos.s13.a2wild.C13Fact;
import chaos.s13.a2wild.validation.C13FactTypeFormatValidator;
import chaos.s13.a2wild.validation.C13FactValidator;
import chaos.s13.a2wild.validation.exists.C13FactOnlyExistsValidator;
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
@RosettaMeta(model=C13Fact.class)
public class C13FactMeta implements RosettaMetaData<C13Fact> {

	@Override
	public List<Validator<? super C13Fact>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C13Fact, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C13Fact> validator(ValidatorFactory factory) {
		return factory.<C13Fact>create(C13FactValidator.class);
	}

	@Override
	public Validator<? super C13Fact> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C13Fact>create(C13FactTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C13Fact> validator() {
		return new C13FactValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C13Fact> typeFormatValidator() {
		return new C13FactTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C13Fact, Set<String>> onlyExistsValidator() {
		return new C13FactOnlyExistsValidator();
	}
}
