package chaos.s09.a4none.meta;

import chaos.s09.a4none.C9Plain;
import chaos.s09.a4none.validation.C9PlainTypeFormatValidator;
import chaos.s09.a4none.validation.C9PlainValidator;
import chaos.s09.a4none.validation.exists.C9PlainOnlyExistsValidator;
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
 * @version 0.0.0
 */
@RosettaMeta(model=C9Plain.class)
public class C9PlainMeta implements RosettaMetaData<C9Plain> {

	@Override
	public List<Validator<? super C9Plain>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C9Plain, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C9Plain> validator(ValidatorFactory factory) {
		return factory.<C9Plain>create(C9PlainValidator.class);
	}

	@Override
	public Validator<? super C9Plain> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C9Plain>create(C9PlainTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C9Plain> validator() {
		return new C9PlainValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C9Plain> typeFormatValidator() {
		return new C9PlainTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C9Plain, Set<String>> onlyExistsValidator() {
		return new C9PlainOnlyExistsValidator();
	}
}
