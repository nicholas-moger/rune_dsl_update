package holdout.typenamedutil.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedutil.ArrayList;
import holdout.typenamedutil.Arrays;
import holdout.typenamedutil.BigDecimal;
import holdout.typenamedutil.Collections;
import holdout.typenamedutil.Collectors;
import holdout.typenamedutil.Consumer;
import holdout.typenamedutil.Function;
import holdout.typenamedutil.Objects;
import holdout.typenamedutil.Pattern;
import holdout.typenamedutil.UtilRefs;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class UtilRefsOnlyExistsValidator implements ValidatorWithArg<UtilRefs, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends UtilRefs> ValidationResult<UtilRefs> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("map", ExistenceChecker.isSet((holdout.typenamedutil.Map) o.getMap()))
				.put("theSet", ExistenceChecker.isSet((holdout.typenamedutil.Set) o.getTheSet()))
				.put("objects", ExistenceChecker.isSet((Objects) o.getObjects()))
				.put("collectors", ExistenceChecker.isSet((Collectors) o.getCollectors()))
				.put("arrays", ExistenceChecker.isSet((Arrays) o.getArrays()))
				.put("collections", ExistenceChecker.isSet((Collections) o.getCollections()))
				.put("fn", ExistenceChecker.isSet((Function) o.getFn()))
				.put("consumer", ExistenceChecker.isSet((Consumer) o.getConsumer()))
				.put("arrayList", ExistenceChecker.isSet((ArrayList) o.getArrayList()))
				.put("pat", ExistenceChecker.isSet((Pattern) o.getPat()))
				.put("bigDecimal", ExistenceChecker.isSet((BigDecimal) o.getBigDecimal()))
				.put("names", ExistenceChecker.isSet((List<String>) o.getNames()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(java.util.stream.Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("UtilRefs", ValidationResult.ValidationType.ONLY_EXISTS, "UtilRefs", path, "");
		}
		return failure("UtilRefs", ValidationResult.ValidationType.ONLY_EXISTS, "UtilRefs", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
