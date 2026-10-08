package chaos.s21.a2qual.validation.exists;

import chaos.s21.a2qual.C21Exactly;
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

public class C21ExactlyOnlyExistsValidator implements ValidatorWithArg<C21Exactly, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C21Exactly> ValidationResult<C21Exactly> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("a", ExistenceChecker.isSet((String) o.getA()))
				.put("b", ExistenceChecker.isSet((String) o.getB()))
				.put("c", ExistenceChecker.isSet((String) o.getC()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C21Exactly", ValidationResult.ValidationType.ONLY_EXISTS, "C21Exactly", path, "");
		}
		return failure("C21Exactly", ValidationResult.ValidationType.ONLY_EXISTS, "C21Exactly", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
