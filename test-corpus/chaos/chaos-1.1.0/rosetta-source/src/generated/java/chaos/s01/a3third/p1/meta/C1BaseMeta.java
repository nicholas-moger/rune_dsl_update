package chaos.s01.a3third.p1.meta;

import chaos.s01.a3third.p1.C1Base;
import chaos.s01.a3third.p1.validation.C1BaseTypeFormatValidator;
import chaos.s01.a3third.p1.validation.C1BaseValidator;
import chaos.s01.a3third.p1.validation.exists.C1BaseOnlyExistsValidator;
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
@RosettaMeta(model=C1Base.class)
public class C1BaseMeta implements RosettaMetaData<C1Base> {

	@Override
	public List<Validator<? super C1Base>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C1Base, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C1Base> validator(ValidatorFactory factory) {
		return factory.<C1Base>create(C1BaseValidator.class);
	}

	@Override
	public Validator<? super C1Base> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C1Base>create(C1BaseTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C1Base> validator() {
		return new C1BaseValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C1Base> typeFormatValidator() {
		return new C1BaseTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C1Base, Set<String>> onlyExistsValidator() {
		return new C1BaseOnlyExistsValidator();
	}
}
