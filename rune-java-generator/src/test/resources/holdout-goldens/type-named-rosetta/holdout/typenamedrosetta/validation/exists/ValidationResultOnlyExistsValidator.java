package holdout.typenamedrosetta.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedrosetta.ValidationResult;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class ValidationResultOnlyExistsValidator implements ValidatorWithArg<ValidationResult, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends ValidationResult> com.rosetta.model.lib.validation.ValidationResult<ValidationResult> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("xs", ExistenceChecker.isSet((List<String>) o.getXs()))
				.put("x", ExistenceChecker.isSet((String) o.getX()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("ValidationResult", com.rosetta.model.lib.validation.ValidationResult.ValidationType.ONLY_EXISTS, "ValidationResult", path, "");
		}
		return failure("ValidationResult", com.rosetta.model.lib.validation.ValidationResult.ValidationType.ONLY_EXISTS, "ValidationResult", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
