package chaos.s19.a3hub.p1.meta;

import chaos.s19.a3hub.p1.C19Part;
import chaos.s19.a3hub.p1.validation.C19PartTypeFormatValidator;
import chaos.s19.a3hub.p1.validation.C19PartValidator;
import chaos.s19.a3hub.p1.validation.exists.C19PartOnlyExistsValidator;
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
@RosettaMeta(model=C19Part.class)
public class C19PartMeta implements RosettaMetaData<C19Part> {

	@Override
	public List<Validator<? super C19Part>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C19Part, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C19Part> validator(ValidatorFactory factory) {
		return factory.<C19Part>create(C19PartValidator.class);
	}

	@Override
	public Validator<? super C19Part> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C19Part>create(C19PartTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C19Part> validator() {
		return new C19PartValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C19Part> typeFormatValidator() {
		return new C19PartTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C19Part, Set<String>> onlyExistsValidator() {
		return new C19PartOnlyExistsValidator();
	}
}
