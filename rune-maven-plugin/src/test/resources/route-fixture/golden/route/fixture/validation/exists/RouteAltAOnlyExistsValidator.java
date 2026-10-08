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
import route.fixture.RouteLeaf;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class RouteAltAOnlyExistsValidator implements ValidatorWithArg<RouteAltA, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends RouteAltA> ValidationResult<RouteAltA> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("shared", ExistenceChecker.isSet((RouteLeaf) o.getShared()))
				.put("p", ExistenceChecker.isSet((String) o.getP()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("RouteAltA", ValidationResult.ValidationType.ONLY_EXISTS, "RouteAltA", path, "");
		}
		return failure("RouteAltA", ValidationResult.ValidationType.ONLY_EXISTS, "RouteAltA", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
