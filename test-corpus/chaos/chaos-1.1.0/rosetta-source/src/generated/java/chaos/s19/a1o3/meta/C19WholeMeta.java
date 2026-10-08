package chaos.s19.a1o3.meta;

import chaos.s19.a1o3.C19Whole;
import chaos.s19.a1o3.validation.C19WholeTypeFormatValidator;
import chaos.s19.a1o3.validation.C19WholeValidator;
import chaos.s19.a1o3.validation.exists.C19WholeOnlyExistsValidator;
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
@RosettaMeta(model=C19Whole.class)
public class C19WholeMeta implements RosettaMetaData<C19Whole> {

	@Override
	public List<Validator<? super C19Whole>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C19Whole, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C19Whole> validator(ValidatorFactory factory) {
		return factory.<C19Whole>create(C19WholeValidator.class);
	}

	@Override
	public Validator<? super C19Whole> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C19Whole>create(C19WholeTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C19Whole> validator() {
		return new C19WholeValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C19Whole> typeFormatValidator() {
		return new C19WholeTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C19Whole, Set<String>> onlyExistsValidator() {
		return new C19WholeOnlyExistsValidator();
	}
}
