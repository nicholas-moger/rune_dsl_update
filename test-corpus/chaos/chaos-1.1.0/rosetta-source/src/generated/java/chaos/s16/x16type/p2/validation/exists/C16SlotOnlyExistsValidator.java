package chaos.s16.x16type.p2.validation.exists;

import chaos.s16.x16type.p1.C16Held;
import chaos.s16.x16type.p2.C16Alpha;
import chaos.s16.x16type.p2.C16Slot;
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

public class C16SlotOnlyExistsValidator implements ValidatorWithArg<C16Slot, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C16Slot> ValidationResult<C16Slot> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("firstPick", ExistenceChecker.isSet((C16Alpha) o.getFirstPick()))
				.put("held", ExistenceChecker.isSet((C16Held) o.getHeld()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C16Slot", ValidationResult.ValidationType.ONLY_EXISTS, "C16Slot", path, "");
		}
		return failure("C16Slot", ValidationResult.ValidationType.ONLY_EXISTS, "C16Slot", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
