package chaos.s11.a1o3.meta;

import chaos.s11.a1o3.C11Mapped;
import chaos.s11.a1o3.validation.C11MappedTypeFormatValidator;
import chaos.s11.a1o3.validation.C11MappedValidator;
import chaos.s11.a1o3.validation.exists.C11MappedOnlyExistsValidator;
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
@RosettaMeta(model=C11Mapped.class)
public class C11MappedMeta implements RosettaMetaData<C11Mapped> {

	@Override
	public List<Validator<? super C11Mapped>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C11Mapped, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C11Mapped> validator(ValidatorFactory factory) {
		return factory.<C11Mapped>create(C11MappedValidator.class);
	}

	@Override
	public Validator<? super C11Mapped> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C11Mapped>create(C11MappedTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C11Mapped> validator() {
		return new C11MappedValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C11Mapped> typeFormatValidator() {
		return new C11MappedTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C11Mapped, Set<String>> onlyExistsValidator() {
		return new C11MappedOnlyExistsValidator();
	}
}
