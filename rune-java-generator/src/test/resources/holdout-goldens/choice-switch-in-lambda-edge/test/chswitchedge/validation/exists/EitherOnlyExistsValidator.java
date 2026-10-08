package test.chswitchedge.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import test.chswitchedge.Either;
import test.chswitchedge.OptA;
import test.chswitchedge.OptB;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class EitherOnlyExistsValidator implements ValidatorWithArg<Either, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends Either> ValidationResult<Either> validate(RosettaPath path, T2 o, Set<String> fields) {
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
			return success("Either", ValidationResult.ValidationType.ONLY_EXISTS, "Either", path, "");
		}
		return failure("Either", ValidationResult.ValidationType.ONLY_EXISTS, "Either", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
