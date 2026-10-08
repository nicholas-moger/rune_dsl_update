package chaos.s01.a3half.p1.meta;

import chaos.s01.a3half.p1.C1Leaf;
import chaos.s01.a3half.p1.validation.C1LeafTypeFormatValidator;
import chaos.s01.a3half.p1.validation.C1LeafValidator;
import chaos.s01.a3half.p1.validation.exists.C1LeafOnlyExistsValidator;
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
@RosettaMeta(model=C1Leaf.class)
public class C1LeafMeta implements RosettaMetaData<C1Leaf> {

	@Override
	public List<Validator<? super C1Leaf>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C1Leaf, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C1Leaf> validator(ValidatorFactory factory) {
		return factory.<C1Leaf>create(C1LeafValidator.class);
	}

	@Override
	public Validator<? super C1Leaf> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C1Leaf>create(C1LeafTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C1Leaf> validator() {
		return new C1LeafValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C1Leaf> typeFormatValidator() {
		return new C1LeafTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C1Leaf, Set<String>> onlyExistsValidator() {
		return new C1LeafOnlyExistsValidator();
	}
}
