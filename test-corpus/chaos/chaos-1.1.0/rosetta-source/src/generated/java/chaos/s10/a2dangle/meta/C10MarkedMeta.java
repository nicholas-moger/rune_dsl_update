package chaos.s10.a2dangle.meta;

import chaos.s10.a2dangle.C10Marked;
import chaos.s10.a2dangle.validation.C10MarkedTypeFormatValidator;
import chaos.s10.a2dangle.validation.C10MarkedValidator;
import chaos.s10.a2dangle.validation.exists.C10MarkedOnlyExistsValidator;
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
@RosettaMeta(model=C10Marked.class)
public class C10MarkedMeta implements RosettaMetaData<C10Marked> {

	@Override
	public List<Validator<? super C10Marked>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C10Marked, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C10Marked> validator(ValidatorFactory factory) {
		return factory.<C10Marked>create(C10MarkedValidator.class);
	}

	@Override
	public Validator<? super C10Marked> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C10Marked>create(C10MarkedTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C10Marked> validator() {
		return new C10MarkedValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C10Marked> typeFormatValidator() {
		return new C10MarkedTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C10Marked, Set<String>> onlyExistsValidator() {
		return new C10MarkedOnlyExistsValidator();
	}
}
