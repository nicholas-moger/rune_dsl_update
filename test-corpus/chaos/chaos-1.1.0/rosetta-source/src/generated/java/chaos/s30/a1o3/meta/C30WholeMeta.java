package chaos.s30.a1o3.meta;

import chaos.s30.a1o3.C30Whole;
import chaos.s30.a1o3.validation.C30WholeTypeFormatValidator;
import chaos.s30.a1o3.validation.C30WholeValidator;
import chaos.s30.a1o3.validation.exists.C30WholeOnlyExistsValidator;
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
@RosettaMeta(model=C30Whole.class)
public class C30WholeMeta implements RosettaMetaData<C30Whole> {

	@Override
	public List<Validator<? super C30Whole>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C30Whole, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C30Whole> validator(ValidatorFactory factory) {
		return factory.<C30Whole>create(C30WholeValidator.class);
	}

	@Override
	public Validator<? super C30Whole> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C30Whole>create(C30WholeTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C30Whole> validator() {
		return new C30WholeValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C30Whole> typeFormatValidator() {
		return new C30WholeTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C30Whole, Set<String>> onlyExistsValidator() {
		return new C30WholeOnlyExistsValidator();
	}
}
