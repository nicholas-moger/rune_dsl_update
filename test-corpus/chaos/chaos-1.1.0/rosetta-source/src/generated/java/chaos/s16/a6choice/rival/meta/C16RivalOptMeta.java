package chaos.s16.a6choice.rival.meta;

import chaos.s16.a6choice.rival.C16RivalOpt;
import chaos.s16.a6choice.rival.validation.C16RivalOptTypeFormatValidator;
import chaos.s16.a6choice.rival.validation.C16RivalOptValidator;
import chaos.s16.a6choice.rival.validation.exists.C16RivalOptOnlyExistsValidator;
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
@RosettaMeta(model=C16RivalOpt.class)
public class C16RivalOptMeta implements RosettaMetaData<C16RivalOpt> {

	@Override
	public List<Validator<? super C16RivalOpt>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C16RivalOpt, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C16RivalOpt> validator(ValidatorFactory factory) {
		return factory.<C16RivalOpt>create(C16RivalOptValidator.class);
	}

	@Override
	public Validator<? super C16RivalOpt> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C16RivalOpt>create(C16RivalOptTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C16RivalOpt> validator() {
		return new C16RivalOptValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C16RivalOpt> typeFormatValidator() {
		return new C16RivalOptTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C16RivalOpt, Set<String>> onlyExistsValidator() {
		return new C16RivalOptOnlyExistsValidator();
	}
}
