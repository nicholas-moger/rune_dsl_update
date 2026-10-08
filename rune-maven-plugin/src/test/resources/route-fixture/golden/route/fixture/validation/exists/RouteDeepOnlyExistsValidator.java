package route.fixture.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import route.fixture.RouteAltA;
import route.fixture.RouteAltB;
import route.fixture.RouteDeep;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class RouteDeepOnlyExistsValidator implements ValidatorWithArg<RouteDeep, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends RouteDeep> ValidationResult<RouteDeep> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("a", ExistenceChecker.isSet((RouteAltA) o.getA()))
				.put("b", ExistenceChecker.isSet((RouteAltB) o.getB()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("RouteDeep", ValidationResult.ValidationType.ONLY_EXISTS, "RouteDeep", path, "");
		}
		return failure("RouteDeep", ValidationResult.ValidationType.ONLY_EXISTS, "RouteDeep", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
