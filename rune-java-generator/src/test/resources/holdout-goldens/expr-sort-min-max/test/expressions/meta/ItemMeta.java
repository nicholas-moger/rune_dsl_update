package test.expressions.meta;

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
import test.expressions.Item;
import test.expressions.validation.ItemTypeFormatValidator;
import test.expressions.validation.ItemValidator;
import test.expressions.validation.exists.ItemOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Item.class)
public class ItemMeta implements RosettaMetaData<Item> {

	@Override
	public List<Validator<? super Item>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Item, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Item> validator(ValidatorFactory factory) {
		return factory.<Item>create(ItemValidator.class);
	}

	@Override
	public Validator<? super Item> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Item>create(ItemTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Item> validator() {
		return new ItemValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Item> typeFormatValidator() {
		return new ItemTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Item, Set<String>> onlyExistsValidator() {
		return new ItemOnlyExistsValidator();
	}
}
