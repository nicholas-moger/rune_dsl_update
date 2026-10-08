package holdout.listliteraladditemcoerce.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.listliteraladditemcoerce.Box;
import holdout.listliteraladditemcoerce.validation.BoxTypeFormatValidator;
import holdout.listliteraladditemcoerce.validation.BoxValidator;
import holdout.listliteraladditemcoerce.validation.exists.BoxOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Box.class)
public class BoxMeta implements RosettaMetaData<Box> {

	@Override
	public List<Validator<? super Box>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Box, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Box> validator(ValidatorFactory factory) {
		return factory.<Box>create(BoxValidator.class);
	}

	@Override
	public Validator<? super Box> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Box>create(BoxTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Box> validator() {
		return new BoxValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Box> typeFormatValidator() {
		return new BoxTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Box, Set<String>> onlyExistsValidator() {
		return new BoxOnlyExistsValidator();
	}
}
