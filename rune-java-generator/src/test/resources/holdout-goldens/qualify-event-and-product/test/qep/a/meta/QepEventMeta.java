package test.qep.a.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import test.qep.a.QepEvent;
import test.qep.a.functions.Qualify_QepE;
import test.qep.a.validation.QepEventTypeFormatValidator;
import test.qep.a.validation.QepEventValidator;
import test.qep.a.validation.exists.QepEventOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=QepEvent.class)
public class QepEventMeta implements RosettaMetaData<QepEvent> {

	@Override
	public List<Validator<? super QepEvent>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super QepEvent, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Arrays.asList(
			factory.<QepEvent>create(Qualify_QepE.class)
		);
	}
	
	@Override
	public Validator<? super QepEvent> validator(ValidatorFactory factory) {
		return factory.<QepEvent>create(QepEventValidator.class);
	}

	@Override
	public Validator<? super QepEvent> typeFormatValidator(ValidatorFactory factory) {
		return factory.<QepEvent>create(QepEventTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super QepEvent> validator() {
		return new QepEventValidator();
	}

	@Deprecated
	@Override
	public Validator<? super QepEvent> typeFormatValidator() {
		return new QepEventTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super QepEvent, Set<String>> onlyExistsValidator() {
		return new QepEventOnlyExistsValidator();
	}
}
