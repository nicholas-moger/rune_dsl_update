package chaos.s09.a3third.p2.meta;

import chaos.s09.a3third.p2.C9Holder;
import chaos.s09.a3third.p2.validation.C9HolderTypeFormatValidator;
import chaos.s09.a3third.p2.validation.C9HolderValidator;
import chaos.s09.a3third.p2.validation.exists.C9HolderOnlyExistsValidator;
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
@RosettaMeta(model=C9Holder.class)
public class C9HolderMeta implements RosettaMetaData<C9Holder> {

	@Override
	public List<Validator<? super C9Holder>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C9Holder, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C9Holder> validator(ValidatorFactory factory) {
		return factory.<C9Holder>create(C9HolderValidator.class);
	}

	@Override
	public Validator<? super C9Holder> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C9Holder>create(C9HolderTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C9Holder> validator() {
		return new C9HolderValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C9Holder> typeFormatValidator() {
		return new C9HolderTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C9Holder, Set<String>> onlyExistsValidator() {
		return new C9HolderOnlyExistsValidator();
	}
}
