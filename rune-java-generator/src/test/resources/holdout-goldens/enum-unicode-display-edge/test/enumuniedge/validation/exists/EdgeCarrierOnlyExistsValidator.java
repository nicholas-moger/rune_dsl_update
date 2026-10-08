package test.enumuniedge.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import test.enumuniedge.EdgeCarrier;
import test.enumuniedge.EscapeEnum;
import test.enumuniedge.SynonymEnum;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class EdgeCarrierOnlyExistsValidator implements ValidatorWithArg<EdgeCarrier, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends EdgeCarrier> ValidationResult<EdgeCarrier> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("esc", ExistenceChecker.isSet((EscapeEnum) o.getEsc()))
				.put("syn", ExistenceChecker.isSet((SynonymEnum) o.getSyn()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("EdgeCarrier", ValidationResult.ValidationType.ONLY_EXISTS, "EdgeCarrier", path, "");
		}
		return failure("EdgeCarrier", ValidationResult.ValidationType.ONLY_EXISTS, "EdgeCarrier", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
