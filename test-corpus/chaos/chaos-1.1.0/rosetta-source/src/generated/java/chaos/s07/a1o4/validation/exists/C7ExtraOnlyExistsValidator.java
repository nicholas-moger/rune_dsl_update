package chaos.s07.a1o4.validation.exists;

import chaos.s07.a1o4.C7Extra;
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

public class C7ExtraOnlyExistsValidator implements ValidatorWithArg<C7Extra, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C7Extra> ValidationResult<C7Extra> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("memo", ExistenceChecker.isSet((String) o.getMemo()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C7Extra", ValidationResult.ValidationType.ONLY_EXISTS, "C7Extra", path, "");
		}
		return failure("C7Extra", ValidationResult.ValidationType.ONLY_EXISTS, "C7Extra", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
