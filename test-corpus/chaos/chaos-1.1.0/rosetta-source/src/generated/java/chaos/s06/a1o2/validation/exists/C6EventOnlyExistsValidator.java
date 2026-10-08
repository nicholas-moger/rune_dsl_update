package chaos.s06.a1o2.validation.exists;

import chaos.s06.a1o2.C6Event;
import chaos.s06.a1o2.C6Tag;
import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import com.rosetta.model.metafields.FieldWithMetaString;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class C6EventOnlyExistsValidator implements ValidatorWithArg<C6Event, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C6Event> ValidationResult<C6Event> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("eventId", ExistenceChecker.isSet((String) o.getEventId()))
				.put("qty", ExistenceChecker.isSet((BigDecimal) o.getQty()))
				.put("legs", ExistenceChecker.isSet((List<String>) o.getLegs()))
				.put("mark", ExistenceChecker.isSet((C6Tag) o.getMark()))
				.put("venue", ExistenceChecker.isSet((FieldWithMetaString) o.getVenue()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C6Event", ValidationResult.ValidationType.ONLY_EXISTS, "C6Event", path, "");
		}
		return failure("C6Event", ValidationResult.ValidationType.ONLY_EXISTS, "C6Event", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
