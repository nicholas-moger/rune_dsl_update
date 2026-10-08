package chaos.s27.a3hub.p2.meta;

import chaos.s27.a3hub.p2.C27Holder;
import chaos.s27.a3hub.p2.validation.C27HolderTypeFormatValidator;
import chaos.s27.a3hub.p2.validation.C27HolderValidator;
import chaos.s27.a3hub.p2.validation.exists.C27HolderOnlyExistsValidator;
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
@RosettaMeta(model=C27Holder.class)
public class C27HolderMeta implements RosettaMetaData<C27Holder> {

	@Override
	public List<Validator<? super C27Holder>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C27Holder, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C27Holder> validator(ValidatorFactory factory) {
		return factory.<C27Holder>create(C27HolderValidator.class);
	}

	@Override
	public Validator<? super C27Holder> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C27Holder>create(C27HolderTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C27Holder> validator() {
		return new C27HolderValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C27Holder> typeFormatValidator() {
		return new C27HolderTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C27Holder, Set<String>> onlyExistsValidator() {
		return new C27HolderOnlyExistsValidator();
	}
}
