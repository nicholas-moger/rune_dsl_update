package chaos.s12.a2qual.h.validation.exists;

import chaos.s12.a2qual.h.C12Aux;
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

public class C12AuxOnlyExistsValidator implements ValidatorWithArg<C12Aux, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C12Aux> ValidationResult<C12Aux> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("ax", ExistenceChecker.isSet((String) o.getAx()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C12Aux", ValidationResult.ValidationType.ONLY_EXISTS, "C12Aux", path, "");
		}
		return failure("C12Aux", ValidationResult.ValidationType.ONLY_EXISTS, "C12Aux", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
