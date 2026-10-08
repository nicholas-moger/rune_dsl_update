package chaos.s27.a9tabs.meta;

import chaos.s27.a9tabs.C27Ref;
import chaos.s27.a9tabs.validation.C27RefTypeFormatValidator;
import chaos.s27.a9tabs.validation.C27RefValidator;
import chaos.s27.a9tabs.validation.exists.C27RefOnlyExistsValidator;
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
@RosettaMeta(model=C27Ref.class)
public class C27RefMeta implements RosettaMetaData<C27Ref> {

	@Override
	public List<Validator<? super C27Ref>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C27Ref, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C27Ref> validator(ValidatorFactory factory) {
		return factory.<C27Ref>create(C27RefValidator.class);
	}

	@Override
	public Validator<? super C27Ref> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C27Ref>create(C27RefTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C27Ref> validator() {
		return new C27RefValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C27Ref> typeFormatValidator() {
		return new C27RefTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C27Ref, Set<String>> onlyExistsValidator() {
		return new C27RefOnlyExistsValidator();
	}
}
