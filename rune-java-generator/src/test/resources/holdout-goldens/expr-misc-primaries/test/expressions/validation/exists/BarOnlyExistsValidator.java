package test.expressions.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import test.expressions.Bar;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class BarOnlyExistsValidator implements ValidatorWithArg<Bar, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends Bar> ValidationResult<Bar> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("x", ExistenceChecker.isSet((BigDecimal) o.getX()))
				.put("y", ExistenceChecker.isSet((BigDecimal) o.getY()))
				.put("z", ExistenceChecker.isSet((List<BigDecimal>) o.getZ()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("Bar", ValidationResult.ValidationType.ONLY_EXISTS, "Bar", path, "");
		}
		return failure("Bar", ValidationResult.ValidationType.ONLY_EXISTS, "Bar", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
