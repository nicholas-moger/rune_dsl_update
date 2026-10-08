package chaos.s28.a2dangle.meta;

import chaos.s28.a2dangle.C28Which;
import chaos.s28.a2dangle.validation.C28WhichTypeFormatValidator;
import chaos.s28.a2dangle.validation.C28WhichValidator;
import chaos.s28.a2dangle.validation.datarule.C28WhichChoice;
import chaos.s28.a2dangle.validation.exists.C28WhichOnlyExistsValidator;
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
@RosettaMeta(model=C28Which.class)
public class C28WhichMeta implements RosettaMetaData<C28Which> {

	@Override
	public List<Validator<? super C28Which>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
			factory.<C28Which>create(C28WhichChoice.class)
		);
	}
	
	@Override
	public List<Function<? super C28Which, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C28Which> validator(ValidatorFactory factory) {
		return factory.<C28Which>create(C28WhichValidator.class);
	}

	@Override
	public Validator<? super C28Which> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C28Which>create(C28WhichTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C28Which> validator() {
		return new C28WhichValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C28Which> typeFormatValidator() {
		return new C28WhichTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C28Which, Set<String>> onlyExistsValidator() {
		return new C28WhichOnlyExistsValidator();
	}
}
