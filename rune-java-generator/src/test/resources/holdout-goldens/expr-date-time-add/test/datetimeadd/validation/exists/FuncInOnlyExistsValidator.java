package test.datetimeadd.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.records.Date;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.time.LocalTime;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import test.datetimeadd.FuncIn;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class FuncInOnlyExistsValidator implements ValidatorWithArg<FuncIn, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends FuncIn> ValidationResult<FuncIn> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("val1", ExistenceChecker.isSet((Date) o.getVal1()))
				.put("val2", ExistenceChecker.isSet((LocalTime) o.getVal2()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("FuncIn", ValidationResult.ValidationType.ONLY_EXISTS, "FuncIn", path, "");
		}
		return failure("FuncIn", ValidationResult.ValidationType.ONLY_EXISTS, "FuncIn", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
