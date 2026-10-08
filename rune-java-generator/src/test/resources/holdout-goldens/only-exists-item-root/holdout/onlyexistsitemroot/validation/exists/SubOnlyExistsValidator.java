package holdout.onlyexistsitemroot.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.onlyexistsitemroot.Sub;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class SubOnlyExistsValidator implements ValidatorWithArg<Sub, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends Sub> ValidationResult<Sub> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("sname", ExistenceChecker.isSet((String) o.getSname()))
				.put("subs", ExistenceChecker.isSet((List<String>) o.getSubs()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("Sub", ValidationResult.ValidationType.ONLY_EXISTS, "Sub", path, "");
		}
		return failure("Sub", ValidationResult.ValidationType.ONLY_EXISTS, "Sub", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
