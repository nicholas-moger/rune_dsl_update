package chaos.s28.a2alias.meta;

import chaos.s28.a2alias.C28OptB;
import chaos.s28.a2alias.validation.C28OptBTypeFormatValidator;
import chaos.s28.a2alias.validation.C28OptBValidator;
import chaos.s28.a2alias.validation.exists.C28OptBOnlyExistsValidator;
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
@RosettaMeta(model=C28OptB.class)
public class C28OptBMeta implements RosettaMetaData<C28OptB> {

	@Override
	public List<Validator<? super C28OptB>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C28OptB, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C28OptB> validator(ValidatorFactory factory) {
		return factory.<C28OptB>create(C28OptBValidator.class);
	}

	@Override
	public Validator<? super C28OptB> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C28OptB>create(C28OptBTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C28OptB> validator() {
		return new C28OptBValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C28OptB> typeFormatValidator() {
		return new C28OptBTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C28OptB, Set<String>> onlyExistsValidator() {
		return new C28OptBOnlyExistsValidator();
	}
}
