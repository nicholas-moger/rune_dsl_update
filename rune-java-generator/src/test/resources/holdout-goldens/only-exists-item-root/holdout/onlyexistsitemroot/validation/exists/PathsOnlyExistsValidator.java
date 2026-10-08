package holdout.onlyexistsitemroot.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.onlyexistsitemroot.Paths;
import holdout.onlyexistsitemroot.Pick;
import holdout.onlyexistsitemroot.Sub;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class PathsOnlyExistsValidator implements ValidatorWithArg<Paths, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends Paths> ValidationResult<Paths> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("p", ExistenceChecker.isSet((String) o.getP()))
				.put("q", ExistenceChecker.isSet((String) o.getQ()))
				.put("sub", ExistenceChecker.isSet((Sub) o.getSub()))
				.put("pick", ExistenceChecker.isSet((Pick) o.getPick()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("Paths", ValidationResult.ValidationType.ONLY_EXISTS, "Paths", path, "");
		}
		return failure("Paths", ValidationResult.ValidationType.ONLY_EXISTS, "Paths", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
