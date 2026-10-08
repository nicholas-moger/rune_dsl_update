package chaos.s30.a5mixed.validation.exists;

import chaos.s30.a5mixed.C30Part;
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

public class C30PartOnlyExistsValidator implements ValidatorWithArg<C30Part, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C30Part> ValidationResult<C30Part> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("pid", ExistenceChecker.isSet((String) o.getPid()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C30Part", ValidationResult.ValidationType.ONLY_EXISTS, "C30Part", path, "");
		}
		return failure("C30Part", ValidationResult.ValidationType.ONLY_EXISTS, "C30Part", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
