package test.singletolistset.validation.exists;

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
import test.singletolistset.NumberList;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class NumberListOnlyExistsValidator implements ValidatorWithArg<NumberList, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends NumberList> ValidationResult<NumberList> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("numbers", ExistenceChecker.isSet((List<BigDecimal>) o.getNumbers()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("NumberList", ValidationResult.ValidationType.ONLY_EXISTS, "NumberList", path, "");
		}
		return failure("NumberList", ValidationResult.ValidationType.ONLY_EXISTS, "NumberList", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
