package chaos.s32.a3hub.p2.meta;

import chaos.s32.a3hub.p2.C32Obj;
import chaos.s32.a3hub.p2.validation.C32ObjTypeFormatValidator;
import chaos.s32.a3hub.p2.validation.C32ObjValidator;
import chaos.s32.a3hub.p2.validation.exists.C32ObjOnlyExistsValidator;
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
@RosettaMeta(model=C32Obj.class)
public class C32ObjMeta implements RosettaMetaData<C32Obj> {

	@Override
	public List<Validator<? super C32Obj>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C32Obj, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C32Obj> validator(ValidatorFactory factory) {
		return factory.<C32Obj>create(C32ObjValidator.class);
	}

	@Override
	public Validator<? super C32Obj> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C32Obj>create(C32ObjTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C32Obj> validator() {
		return new C32ObjValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C32Obj> typeFormatValidator() {
		return new C32ObjTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C32Obj, Set<String>> onlyExistsValidator() {
		return new C32ObjOnlyExistsValidator();
	}
}
