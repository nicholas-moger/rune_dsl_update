package chaos.s18.base.meta;

import chaos.s18.base.C18Coded;
import chaos.s18.base.validation.C18CodedTypeFormatValidator;
import chaos.s18.base.validation.C18CodedValidator;
import chaos.s18.base.validation.exists.C18CodedOnlyExistsValidator;
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
@RosettaMeta(model=C18Coded.class)
public class C18CodedMeta implements RosettaMetaData<C18Coded> {

	@Override
	public List<Validator<? super C18Coded>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C18Coded, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C18Coded> validator(ValidatorFactory factory) {
		return factory.<C18Coded>create(C18CodedValidator.class);
	}

	@Override
	public Validator<? super C18Coded> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C18Coded>create(C18CodedTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C18Coded> validator() {
		return new C18CodedValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C18Coded> typeFormatValidator() {
		return new C18CodedTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C18Coded, Set<String>> onlyExistsValidator() {
		return new C18CodedOnlyExistsValidator();
	}
}
