package chaos.s29.a5mixed.meta;

import chaos.s29.a5mixed.C29Leaf;
import chaos.s29.a5mixed.validation.C29LeafTypeFormatValidator;
import chaos.s29.a5mixed.validation.C29LeafValidator;
import chaos.s29.a5mixed.validation.exists.C29LeafOnlyExistsValidator;
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
@RosettaMeta(model=C29Leaf.class)
public class C29LeafMeta implements RosettaMetaData<C29Leaf> {

	@Override
	public List<Validator<? super C29Leaf>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C29Leaf, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C29Leaf> validator(ValidatorFactory factory) {
		return factory.<C29Leaf>create(C29LeafValidator.class);
	}

	@Override
	public Validator<? super C29Leaf> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C29Leaf>create(C29LeafTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C29Leaf> validator() {
		return new C29LeafValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C29Leaf> typeFormatValidator() {
		return new C29LeafTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C29Leaf, Set<String>> onlyExistsValidator() {
		return new C29LeafOnlyExistsValidator();
	}
}
