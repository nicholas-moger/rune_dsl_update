package test.chswitchbare.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import test.chswitchbare.OptA;
import test.chswitchbare.OptB;
import test.chswitchbare.Outer2;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class Outer2OnlyExistsValidator implements ValidatorWithArg<Outer2, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends Outer2> ValidationResult<Outer2> validate(RosettaPath path, T2 o, Set<String> fields) {
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
			return success("Outer2", ValidationResult.ValidationType.ONLY_EXISTS, "Outer2", path, "");
		}
		return failure("Outer2", ValidationResult.ValidationType.ONLY_EXISTS, "Outer2", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
