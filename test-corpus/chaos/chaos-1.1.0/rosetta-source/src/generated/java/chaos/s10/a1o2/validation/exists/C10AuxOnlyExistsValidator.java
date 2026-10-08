package chaos.s10.a1o2.validation.exists;

import chaos.s10.a1o2.C10Aux;
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

public class C10AuxOnlyExistsValidator implements ValidatorWithArg<C10Aux, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C10Aux> ValidationResult<C10Aux> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("aux", ExistenceChecker.isSet((String) o.getAux()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C10Aux", ValidationResult.ValidationType.ONLY_EXISTS, "C10Aux", path, "");
		}
		return failure("C10Aux", ValidationResult.ValidationType.ONLY_EXISTS, "C10Aux", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
