package holdout.typenamedutil.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedutil.Map;
import holdout.typenamedutil.validation.MapTypeFormatValidator;
import holdout.typenamedutil.validation.MapValidator;
import holdout.typenamedutil.validation.exists.MapOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Map.class)
public class MapMeta implements RosettaMetaData<Map> {

	@Override
	public List<Validator<? super Map>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Map, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Map> validator(ValidatorFactory factory) {
		return factory.<Map>create(MapValidator.class);
	}

	@Override
	public Validator<? super Map> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Map>create(MapTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Map> validator() {
		return new MapValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Map> typeFormatValidator() {
		return new MapTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Map, Set<String>> onlyExistsValidator() {
		return new MapOnlyExistsValidator();
	}
}
