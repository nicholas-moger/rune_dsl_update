package chaos.s15.a2qual.validation.exists;

import chaos.s15.a2qual.C15Carrier;
import chaos.s15.a2qual.h.C15Aux;
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

public class C15CarrierOnlyExistsValidator implements ValidatorWithArg<C15Carrier, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C15Carrier> ValidationResult<C15Carrier> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("tok", ExistenceChecker.isSet((Void) o.getTok()))
				.put("span", ExistenceChecker.isSet((Void) o.getSpan()))
				.put("aux", ExistenceChecker.isSet((C15Aux) o.getAux()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C15Carrier", ValidationResult.ValidationType.ONLY_EXISTS, "C15Carrier", path, "");
		}
		return failure("C15Carrier", ValidationResult.ValidationType.ONLY_EXISTS, "C15Carrier", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
