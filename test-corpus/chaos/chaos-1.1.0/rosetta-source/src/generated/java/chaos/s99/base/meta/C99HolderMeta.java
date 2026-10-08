package chaos.s99.base.meta;

import chaos.s99.base.C99Holder;
import chaos.s99.base.validation.C99HolderTypeFormatValidator;
import chaos.s99.base.validation.C99HolderValidator;
import chaos.s99.base.validation.exists.C99HolderOnlyExistsValidator;
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
@RosettaMeta(model=C99Holder.class)
public class C99HolderMeta implements RosettaMetaData<C99Holder> {

	@Override
	public List<Validator<? super C99Holder>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C99Holder, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C99Holder> validator(ValidatorFactory factory) {
		return factory.<C99Holder>create(C99HolderValidator.class);
	}

	@Override
	public Validator<? super C99Holder> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C99Holder>create(C99HolderTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C99Holder> validator() {
		return new C99HolderValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C99Holder> typeFormatValidator() {
		return new C99HolderTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C99Holder, Set<String>> onlyExistsValidator() {
		return new C99HolderOnlyExistsValidator();
	}
}
