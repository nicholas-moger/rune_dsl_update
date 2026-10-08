package chaos.s95.base.meta;

import chaos.s95.base.C95Item;
import chaos.s95.base.validation.C95ItemTypeFormatValidator;
import chaos.s95.base.validation.C95ItemValidator;
import chaos.s95.base.validation.exists.C95ItemOnlyExistsValidator;
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
@RosettaMeta(model=C95Item.class)
public class C95ItemMeta implements RosettaMetaData<C95Item> {

	@Override
	public List<Validator<? super C95Item>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C95Item, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C95Item> validator(ValidatorFactory factory) {
		return factory.<C95Item>create(C95ItemValidator.class);
	}

	@Override
	public Validator<? super C95Item> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C95Item>create(C95ItemTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C95Item> validator() {
		return new C95ItemValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C95Item> typeFormatValidator() {
		return new C95ItemTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C95Item, Set<String>> onlyExistsValidator() {
		return new C95ItemOnlyExistsValidator();
	}
}
