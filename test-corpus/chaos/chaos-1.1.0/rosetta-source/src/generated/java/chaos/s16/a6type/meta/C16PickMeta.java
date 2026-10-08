package chaos.s16.a6type.meta;

import chaos.s16.a6type.C16Pick;
import chaos.s16.a6type.validation.C16PickTypeFormatValidator;
import chaos.s16.a6type.validation.C16PickValidator;
import chaos.s16.a6type.validation.datarule.C16PickChoice;
import chaos.s16.a6type.validation.exists.C16PickOnlyExistsValidator;
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
@RosettaMeta(model=C16Pick.class)
public class C16PickMeta implements RosettaMetaData<C16Pick> {

	@Override
	public List<Validator<? super C16Pick>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
			factory.<C16Pick>create(C16PickChoice.class)
		);
	}
	
	@Override
	public List<Function<? super C16Pick, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C16Pick> validator(ValidatorFactory factory) {
		return factory.<C16Pick>create(C16PickValidator.class);
	}

	@Override
	public Validator<? super C16Pick> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C16Pick>create(C16PickTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C16Pick> validator() {
		return new C16PickValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C16Pick> typeFormatValidator() {
		return new C16PickTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C16Pick, Set<String>> onlyExistsValidator() {
		return new C16PickOnlyExistsValidator();
	}
}
