package chaos.s09.a1o1.validation.exists;

import chaos.s09.a1o1.C9Plain;
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

public class C9PlainOnlyExistsValidator implements ValidatorWithArg<C9Plain, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C9Plain> ValidationResult<C9Plain> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("p", ExistenceChecker.isSet((String) o.getP()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C9Plain", ValidationResult.ValidationType.ONLY_EXISTS, "C9Plain", path, "");
		}
		return failure("C9Plain", ValidationResult.ValidationType.ONLY_EXISTS, "C9Plain", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
