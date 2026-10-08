package test.aliascond.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import com.rosetta.model.metafields.FieldWithMetaString;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import test.aliascond.Bare;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class BareOnlyExistsValidator implements ValidatorWithArg<Bare, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends Bare> ValidationResult<Bare> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("qty", ExistenceChecker.isSet((BigDecimal) o.getQty()))
				.put("text", ExistenceChecker.isSet((FieldWithMetaString) o.getText()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("Bare", ValidationResult.ValidationType.ONLY_EXISTS, "Bare", path, "");
		}
		return failure("Bare", ValidationResult.ValidationType.ONLY_EXISTS, "Bare", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
