package holdout.onlyexistsitemroot.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.onlyexistsitemroot.OptA;
import holdout.onlyexistsitemroot.OptB;
import holdout.onlyexistsitemroot.Pick;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class PickOnlyExistsValidator implements ValidatorWithArg<Pick, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends Pick> ValidationResult<Pick> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("OptA", ExistenceChecker.isSet((OptA) o.getOptA()))
				.put("OptB", ExistenceChecker.isSet((OptB) o.getOptB()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("Pick", ValidationResult.ValidationType.ONLY_EXISTS, "Pick", path, "");
		}
		return failure("Pick", ValidationResult.ValidationType.ONLY_EXISTS, "Pick", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
