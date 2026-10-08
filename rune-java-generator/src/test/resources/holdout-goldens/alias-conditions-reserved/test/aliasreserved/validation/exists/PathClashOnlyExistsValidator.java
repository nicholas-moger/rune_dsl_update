package test.aliasreserved.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import test.aliasreserved.PathClash;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class PathClashOnlyExistsValidator implements ValidatorWithArg<PathClash, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends PathClash> ValidationResult<PathClash> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("pa", ExistenceChecker.isSet((Integer) o.getPa()))
				.put("pas", ExistenceChecker.isSet((List<Integer>) o.getPas()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("PathClash", ValidationResult.ValidationType.ONLY_EXISTS, "PathClash", path, "");
		}
		return failure("PathClash", ValidationResult.ValidationType.ONLY_EXISTS, "PathClash", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
