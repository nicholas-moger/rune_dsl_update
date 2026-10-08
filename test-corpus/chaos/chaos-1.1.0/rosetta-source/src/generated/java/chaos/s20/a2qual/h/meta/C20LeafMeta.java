package chaos.s20.a2qual.h.meta;

import chaos.s20.a2qual.h.C20Leaf;
import chaos.s20.a2qual.h.validation.C20LeafTypeFormatValidator;
import chaos.s20.a2qual.h.validation.C20LeafValidator;
import chaos.s20.a2qual.h.validation.exists.C20LeafOnlyExistsValidator;
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
@RosettaMeta(model=C20Leaf.class)
public class C20LeafMeta implements RosettaMetaData<C20Leaf> {

	@Override
	public List<Validator<? super C20Leaf>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C20Leaf, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C20Leaf> validator(ValidatorFactory factory) {
		return factory.<C20Leaf>create(C20LeafValidator.class);
	}

	@Override
	public Validator<? super C20Leaf> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C20Leaf>create(C20LeafTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C20Leaf> validator() {
		return new C20LeafValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C20Leaf> typeFormatValidator() {
		return new C20LeafTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C20Leaf, Set<String>> onlyExistsValidator() {
		return new C20LeafOnlyExistsValidator();
	}
}
