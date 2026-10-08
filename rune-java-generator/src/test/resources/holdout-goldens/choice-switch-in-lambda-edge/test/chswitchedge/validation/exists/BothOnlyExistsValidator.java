package test.chswitchedge.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import test.chswitchedge.Both;
import test.chswitchedge.Either;
import test.chswitchedge.OptC;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class BothOnlyExistsValidator implements ValidatorWithArg<Both, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends Both> ValidationResult<Both> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("Either", ExistenceChecker.isSet((Either) o.getEither()))
				.put("OptC", ExistenceChecker.isSet((OptC) o.getOptC()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("Both", ValidationResult.ValidationType.ONLY_EXISTS, "Both", path, "");
		}
		return failure("Both", ValidationResult.ValidationType.ONLY_EXISTS, "Both", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
