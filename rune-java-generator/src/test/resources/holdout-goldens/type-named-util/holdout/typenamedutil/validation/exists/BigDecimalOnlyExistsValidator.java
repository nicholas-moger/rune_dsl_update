package holdout.typenamedutil.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedutil.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class BigDecimalOnlyExistsValidator implements ValidatorWithArg<BigDecimal, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends BigDecimal> ValidationResult<BigDecimal> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("xs", ExistenceChecker.isSet((List<String>) o.getXs()))
				.put("n", ExistenceChecker.isSet((java.math.BigDecimal) o.getN()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("BigDecimal", ValidationResult.ValidationType.ONLY_EXISTS, "BigDecimal", path, "");
		}
		return failure("BigDecimal", ValidationResult.ValidationType.ONLY_EXISTS, "BigDecimal", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
