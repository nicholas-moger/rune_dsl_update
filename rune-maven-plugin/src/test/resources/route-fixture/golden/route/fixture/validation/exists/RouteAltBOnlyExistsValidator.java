package route.fixture.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import route.fixture.RouteAltB;
import route.fixture.RouteLeaf;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class RouteAltBOnlyExistsValidator implements ValidatorWithArg<RouteAltB, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends RouteAltB> ValidationResult<RouteAltB> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("shared", ExistenceChecker.isSet((RouteLeaf) o.getShared()))
				.put("q", ExistenceChecker.isSet((String) o.getQ()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("RouteAltB", ValidationResult.ValidationType.ONLY_EXISTS, "RouteAltB", path, "");
		}
		return failure("RouteAltB", ValidationResult.ValidationType.ONLY_EXISTS, "RouteAltB", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
