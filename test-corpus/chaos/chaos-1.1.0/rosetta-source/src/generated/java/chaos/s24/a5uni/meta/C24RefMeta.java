package chaos.s24.a5uni.meta;

import chaos.s24.a5uni.C24Ref;
import chaos.s24.a5uni.validation.C24RefTypeFormatValidator;
import chaos.s24.a5uni.validation.C24RefValidator;
import chaos.s24.a5uni.validation.exists.C24RefOnlyExistsValidator;
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
@RosettaMeta(model=C24Ref.class)
public class C24RefMeta implements RosettaMetaData<C24Ref> {

	@Override
	public List<Validator<? super C24Ref>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C24Ref, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C24Ref> validator(ValidatorFactory factory) {
		return factory.<C24Ref>create(C24RefValidator.class);
	}

	@Override
	public Validator<? super C24Ref> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C24Ref>create(C24RefTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C24Ref> validator() {
		return new C24RefValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C24Ref> typeFormatValidator() {
		return new C24RefTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C24Ref, Set<String>> onlyExistsValidator() {
		return new C24RefOnlyExistsValidator();
	}
}
