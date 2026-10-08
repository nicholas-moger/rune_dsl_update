package chaos.s21.a2wild.h.validation.exists;

import chaos.s21.a2wild.h.C21Aux;
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

public class C21AuxOnlyExistsValidator implements ValidatorWithArg<C21Aux, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C21Aux> ValidationResult<C21Aux> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("ax", ExistenceChecker.isSet((String) o.getAx()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C21Aux", ValidationResult.ValidationType.ONLY_EXISTS, "C21Aux", path, "");
		}
		return failure("C21Aux", ValidationResult.ValidationType.ONLY_EXISTS, "C21Aux", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
