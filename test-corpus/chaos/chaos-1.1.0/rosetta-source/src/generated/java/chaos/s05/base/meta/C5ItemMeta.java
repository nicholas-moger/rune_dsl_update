package chaos.s05.base.meta;

import chaos.s05.base.C5Item;
import chaos.s05.base.validation.C5ItemTypeFormatValidator;
import chaos.s05.base.validation.C5ItemValidator;
import chaos.s05.base.validation.exists.C5ItemOnlyExistsValidator;
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
@RosettaMeta(model=C5Item.class)
public class C5ItemMeta implements RosettaMetaData<C5Item> {

	@Override
	public List<Validator<? super C5Item>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C5Item, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C5Item> validator(ValidatorFactory factory) {
		return factory.<C5Item>create(C5ItemValidator.class);
	}

	@Override
	public Validator<? super C5Item> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C5Item>create(C5ItemTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C5Item> validator() {
		return new C5ItemValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C5Item> typeFormatValidator() {
		return new C5ItemTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C5Item, Set<String>> onlyExistsValidator() {
		return new C5ItemOnlyExistsValidator();
	}
}
