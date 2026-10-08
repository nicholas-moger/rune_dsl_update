package chaos.s23.a5uni.meta;

import chaos.s23.a5uni.C23Box;
import chaos.s23.a5uni.validation.C23BoxTypeFormatValidator;
import chaos.s23.a5uni.validation.C23BoxValidator;
import chaos.s23.a5uni.validation.exists.C23BoxOnlyExistsValidator;
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
@RosettaMeta(model=C23Box.class)
public class C23BoxMeta implements RosettaMetaData<C23Box> {

	@Override
	public List<Validator<? super C23Box>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C23Box, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C23Box> validator(ValidatorFactory factory) {
		return factory.<C23Box>create(C23BoxValidator.class);
	}

	@Override
	public Validator<? super C23Box> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C23Box>create(C23BoxTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C23Box> validator() {
		return new C23BoxValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C23Box> typeFormatValidator() {
		return new C23BoxTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C23Box, Set<String>> onlyExistsValidator() {
		return new C23BoxOnlyExistsValidator();
	}
}
