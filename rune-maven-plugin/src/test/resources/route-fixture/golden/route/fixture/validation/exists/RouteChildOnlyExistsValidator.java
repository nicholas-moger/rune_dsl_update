package route.fixture.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import com.rosetta.model.metafields.FieldWithMetaString;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import route.fixture.RouteChild;
import route.fixture.RouteColour;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class RouteChildOnlyExistsValidator implements ValidatorWithArg<RouteChild, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends RouteChild> ValidationResult<RouteChild> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("name", ExistenceChecker.isSet((String) o.getName()))
				.put("notes", ExistenceChecker.isSet((List<String>) o.getNotes()))
				.put("market", ExistenceChecker.isSet((FieldWithMetaString) o.getMarket()))
				.put("colour", ExistenceChecker.isSet((RouteColour) o.getColour()))
				.put("extra", ExistenceChecker.isSet((String) o.getExtra()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("RouteChild", ValidationResult.ValidationType.ONLY_EXISTS, "RouteChild", path, "");
		}
		return failure("RouteChild", ValidationResult.ValidationType.ONLY_EXISTS, "RouteChild", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
