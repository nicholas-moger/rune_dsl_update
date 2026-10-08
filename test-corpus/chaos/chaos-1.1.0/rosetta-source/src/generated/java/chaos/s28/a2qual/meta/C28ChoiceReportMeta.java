package chaos.s28.a2qual.meta;

import chaos.s28.a2qual.C28ChoiceReport;
import chaos.s28.a2qual.validation.C28ChoiceReportTypeFormatValidator;
import chaos.s28.a2qual.validation.C28ChoiceReportValidator;
import chaos.s28.a2qual.validation.exists.C28ChoiceReportOnlyExistsValidator;
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
@RosettaMeta(model=C28ChoiceReport.class)
public class C28ChoiceReportMeta implements RosettaMetaData<C28ChoiceReport> {

	@Override
	public List<Validator<? super C28ChoiceReport>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C28ChoiceReport, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C28ChoiceReport> validator(ValidatorFactory factory) {
		return factory.<C28ChoiceReport>create(C28ChoiceReportValidator.class);
	}

	@Override
	public Validator<? super C28ChoiceReport> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C28ChoiceReport>create(C28ChoiceReportTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C28ChoiceReport> validator() {
		return new C28ChoiceReportValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C28ChoiceReport> typeFormatValidator() {
		return new C28ChoiceReportTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C28ChoiceReport, Set<String>> onlyExistsValidator() {
		return new C28ChoiceReportOnlyExistsValidator();
	}
}
