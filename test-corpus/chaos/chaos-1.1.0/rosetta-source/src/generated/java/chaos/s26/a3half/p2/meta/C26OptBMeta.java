package chaos.s26.a3half.p2.meta;

import chaos.s26.a3half.p2.C26OptB;
import chaos.s26.a3half.p2.validation.C26OptBTypeFormatValidator;
import chaos.s26.a3half.p2.validation.C26OptBValidator;
import chaos.s26.a3half.p2.validation.exists.C26OptBOnlyExistsValidator;
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
@RosettaMeta(model=C26OptB.class)
public class C26OptBMeta implements RosettaMetaData<C26OptB> {

	@Override
	public List<Validator<? super C26OptB>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C26OptB, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C26OptB> validator(ValidatorFactory factory) {
		return factory.<C26OptB>create(C26OptBValidator.class);
	}

	@Override
	public Validator<? super C26OptB> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C26OptB>create(C26OptBTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C26OptB> validator() {
		return new C26OptBValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C26OptB> typeFormatValidator() {
		return new C26OptBTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C26OptB, Set<String>> onlyExistsValidator() {
		return new C26OptBOnlyExistsValidator();
	}
}
