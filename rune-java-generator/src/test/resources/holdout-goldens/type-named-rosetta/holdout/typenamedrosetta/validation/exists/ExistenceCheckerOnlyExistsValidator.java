package holdout.typenamedrosetta.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedrosetta.ExistenceChecker;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class ExistenceCheckerOnlyExistsValidator implements ValidatorWithArg<ExistenceChecker, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends ExistenceChecker> ValidationResult<ExistenceChecker> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("xs", com.rosetta.model.lib.validation.ExistenceChecker.isSet((List<String>) o.getXs()))
				.put("x", com.rosetta.model.lib.validation.ExistenceChecker.isSet((String) o.getX()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("ExistenceChecker", ValidationResult.ValidationType.ONLY_EXISTS, "ExistenceChecker", path, "");
		}
		return failure("ExistenceChecker", ValidationResult.ValidationType.ONLY_EXISTS, "ExistenceChecker", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
