package chaos.s08.a5mixed.meta;

import chaos.s08.a5mixed.C8Box;
import chaos.s08.a5mixed.validation.C8BoxTypeFormatValidator;
import chaos.s08.a5mixed.validation.C8BoxValidator;
import chaos.s08.a5mixed.validation.exists.C8BoxOnlyExistsValidator;
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
@RosettaMeta(model=C8Box.class)
public class C8BoxMeta implements RosettaMetaData<C8Box> {

	@Override
	public List<Validator<? super C8Box>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C8Box, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C8Box> validator(ValidatorFactory factory) {
		return factory.<C8Box>create(C8BoxValidator.class);
	}

	@Override
	public Validator<? super C8Box> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C8Box>create(C8BoxTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C8Box> validator() {
		return new C8BoxValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C8Box> typeFormatValidator() {
		return new C8BoxTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C8Box, Set<String>> onlyExistsValidator() {
		return new C8BoxOnlyExistsValidator();
	}
}
