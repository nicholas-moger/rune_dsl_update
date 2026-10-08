package holdout.typenamedguava.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedguava.GuavaRefs;
import holdout.typenamedguava.ImmutableList;
import holdout.typenamedguava.Lists;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class GuavaRefsOnlyExistsValidator implements ValidatorWithArg<GuavaRefs, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends GuavaRefs> ValidationResult<GuavaRefs> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("immutableList", ExistenceChecker.isSet((ImmutableList) o.getImmutableList()))
				.put("immutableMap", ExistenceChecker.isSet((holdout.typenamedguava.ImmutableMap) o.getImmutableMap()))
				.put("lists", ExistenceChecker.isSet((Lists) o.getLists()))
				.put("names", ExistenceChecker.isSet((List<String>) o.getNames()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("GuavaRefs", ValidationResult.ValidationType.ONLY_EXISTS, "GuavaRefs", path, "");
		}
		return failure("GuavaRefs", ValidationResult.ValidationType.ONLY_EXISTS, "GuavaRefs", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
