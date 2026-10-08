package chaos.s32.a2alias.meta;

import chaos.s32.a2alias.List;
import chaos.s32.a2alias.validation.ListTypeFormatValidator;
import chaos.s32.a2alias.validation.ListValidator;
import chaos.s32.a2alias.validation.exists.ListOnlyExistsValidator;
import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 1.0.0
 */
@RosettaMeta(model=List.class)
public class ListMeta implements RosettaMetaData<List> {

	@Override
	public java.util.List<Validator<? super List>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public java.util.List<Function<? super List, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super List> validator(ValidatorFactory factory) {
		return factory.<List>create(ListValidator.class);
	}

	@Override
	public Validator<? super List> typeFormatValidator(ValidatorFactory factory) {
		return factory.<List>create(ListTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super List> validator() {
		return new ListValidator();
	}

	@Deprecated
	@Override
	public Validator<? super List> typeFormatValidator() {
		return new ListTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super List, Set<String>> onlyExistsValidator() {
		return new ListOnlyExistsValidator();
	}
}
