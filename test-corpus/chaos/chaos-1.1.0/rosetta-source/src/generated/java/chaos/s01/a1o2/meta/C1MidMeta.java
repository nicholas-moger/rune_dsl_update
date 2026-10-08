package chaos.s01.a1o2.meta;

import chaos.s01.a1o2.C1Mid;
import chaos.s01.a1o2.validation.C1MidTypeFormatValidator;
import chaos.s01.a1o2.validation.C1MidValidator;
import chaos.s01.a1o2.validation.exists.C1MidOnlyExistsValidator;
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
@RosettaMeta(model=C1Mid.class)
public class C1MidMeta implements RosettaMetaData<C1Mid> {

	@Override
	public List<Validator<? super C1Mid>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C1Mid, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C1Mid> validator(ValidatorFactory factory) {
		return factory.<C1Mid>create(C1MidValidator.class);
	}

	@Override
	public Validator<? super C1Mid> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C1Mid>create(C1MidTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C1Mid> validator() {
		return new C1MidValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C1Mid> typeFormatValidator() {
		return new C1MidTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C1Mid, Set<String>> onlyExistsValidator() {
		return new C1MidOnlyExistsValidator();
	}
}
