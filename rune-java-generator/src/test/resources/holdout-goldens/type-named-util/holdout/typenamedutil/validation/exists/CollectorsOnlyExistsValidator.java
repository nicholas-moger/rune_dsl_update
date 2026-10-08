package holdout.typenamedutil.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedutil.Collectors;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class CollectorsOnlyExistsValidator implements ValidatorWithArg<Collectors, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends Collectors> ValidationResult<Collectors> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("xs", ExistenceChecker.isSet((List<String>) o.getXs()))
				.put("x", ExistenceChecker.isSet((String) o.getX()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(java.util.stream.Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("Collectors", ValidationResult.ValidationType.ONLY_EXISTS, "Collectors", path, "");
		}
		return failure("Collectors", ValidationResult.ValidationType.ONLY_EXISTS, "Collectors", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
