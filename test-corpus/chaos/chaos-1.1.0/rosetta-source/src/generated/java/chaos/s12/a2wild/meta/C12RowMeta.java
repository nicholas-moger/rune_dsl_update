package chaos.s12.a2wild.meta;

import chaos.s12.a2wild.C12Row;
import chaos.s12.a2wild.validation.C12RowTypeFormatValidator;
import chaos.s12.a2wild.validation.C12RowValidator;
import chaos.s12.a2wild.validation.exists.C12RowOnlyExistsValidator;
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
@RosettaMeta(model=C12Row.class)
public class C12RowMeta implements RosettaMetaData<C12Row> {

	@Override
	public List<Validator<? super C12Row>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C12Row, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C12Row> validator(ValidatorFactory factory) {
		return factory.<C12Row>create(C12RowValidator.class);
	}

	@Override
	public Validator<? super C12Row> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C12Row>create(C12RowTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C12Row> validator() {
		return new C12RowValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C12Row> typeFormatValidator() {
		return new C12RowTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C12Row, Set<String>> onlyExistsValidator() {
		return new C12RowOnlyExistsValidator();
	}
}
