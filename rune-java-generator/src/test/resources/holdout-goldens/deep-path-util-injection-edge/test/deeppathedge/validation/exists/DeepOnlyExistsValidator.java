package test.deeppathedge.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import test.deeppathedge.Deep;
import test.deeppathedge.Inner;
import test.deeppathedge.Outer;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class DeepOnlyExistsValidator implements ValidatorWithArg<Deep, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends Deep> ValidationResult<Deep> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("pick", ExistenceChecker.isSet((Outer) o.getPick()))
				.put("picks", ExistenceChecker.isSet((List<? extends Inner>) o.getPicks()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("Deep", ValidationResult.ValidationType.ONLY_EXISTS, "Deep", path, "");
		}
		return failure("Deep", ValidationResult.ValidationType.ONLY_EXISTS, "Deep", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
