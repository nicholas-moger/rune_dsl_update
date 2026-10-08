package chaos.s21.a3hub.p2.validation.exists;

import chaos.s21.a3hub.p2.C21Paths;
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

public class C21PathsOnlyExistsValidator implements ValidatorWithArg<C21Paths, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C21Paths> ValidationResult<C21Paths> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("p", ExistenceChecker.isSet((String) o.getP()))
				.put("q", ExistenceChecker.isSet((String) o.getQ()))
				.put("r", ExistenceChecker.isSet((String) o.getR()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C21Paths", ValidationResult.ValidationType.ONLY_EXISTS, "C21Paths", path, "");
		}
		return failure("C21Paths", ValidationResult.ValidationType.ONLY_EXISTS, "C21Paths", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
