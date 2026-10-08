package chaos.s28.a1o1.meta;

import chaos.s28.a1o1.C28Extra;
import chaos.s28.a1o1.validation.C28ExtraTypeFormatValidator;
import chaos.s28.a1o1.validation.C28ExtraValidator;
import chaos.s28.a1o1.validation.exists.C28ExtraOnlyExistsValidator;
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
@RosettaMeta(model=C28Extra.class)
public class C28ExtraMeta implements RosettaMetaData<C28Extra> {

	@Override
	public List<Validator<? super C28Extra>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C28Extra, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C28Extra> validator(ValidatorFactory factory) {
		return factory.<C28Extra>create(C28ExtraValidator.class);
	}

	@Override
	public Validator<? super C28Extra> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C28Extra>create(C28ExtraTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C28Extra> validator() {
		return new C28ExtraValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C28Extra> typeFormatValidator() {
		return new C28ExtraTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C28Extra, Set<String>> onlyExistsValidator() {
		return new C28ExtraOnlyExistsValidator();
	}
}
