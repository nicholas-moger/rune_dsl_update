package holdout.typenamedutil.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedutil.Set;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class SetOnlyExistsValidator implements ValidatorWithArg<Set, java.util.Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends Set> ValidationResult<Set> validate(RosettaPath path, T2 o, java.util.Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("xs", ExistenceChecker.isSet((List<String>) o.getXs()))
				.put("x", ExistenceChecker.isSet((String) o.getX()))
				.build();
		
		// Find the fields that are set
		java.util.Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("Set", ValidationResult.ValidationType.ONLY_EXISTS, "Set", path, "");
		}
		return failure("Set", ValidationResult.ValidationType.ONLY_EXISTS, "Set", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
