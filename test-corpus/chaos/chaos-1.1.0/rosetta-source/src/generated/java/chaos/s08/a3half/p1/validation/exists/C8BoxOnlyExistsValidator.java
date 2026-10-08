package chaos.s08.a3half.p1.validation.exists;

import chaos.s08.a3half.p1.C8Box;
import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class C8BoxOnlyExistsValidator implements ValidatorWithArg<C8Box, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C8Box> ValidationResult<C8Box> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("lid", ExistenceChecker.isSet((String) o.getLid()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C8Box", ValidationResult.ValidationType.ONLY_EXISTS, "C8Box", path, "");
		}
		return failure("C8Box", ValidationResult.ValidationType.ONLY_EXISTS, "C8Box", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
