package base.layer.validation.exists;

import base.layer.Event;
import base.layer.TradeEnum;
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

public class EventOnlyExistsValidator implements ValidatorWithArg<Event, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends Event> ValidationResult<Event> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("trade", ExistenceChecker.isSet((TradeEnum) o.getTrade()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("Event", ValidationResult.ValidationType.ONLY_EXISTS, "Event", path, "");
		}
		return failure("Event", ValidationResult.ValidationType.ONLY_EXISTS, "Event", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
