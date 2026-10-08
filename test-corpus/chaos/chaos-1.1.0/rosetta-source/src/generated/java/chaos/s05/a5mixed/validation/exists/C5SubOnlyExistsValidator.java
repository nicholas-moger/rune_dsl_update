package chaos.s05.a5mixed.validation.exists;

import chaos.s05.a5mixed.C5Sub;
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

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class C5SubOnlyExistsValidator implements ValidatorWithArg<C5Sub, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C5Sub> ValidationResult<C5Sub> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("vals", ExistenceChecker.isSet((List<BigDecimal>) o.getVals()))
				.put("name", ExistenceChecker.isSet((String) o.getName()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C5Sub", ValidationResult.ValidationType.ONLY_EXISTS, "C5Sub", path, "");
		}
		return failure("C5Sub", ValidationResult.ValidationType.ONLY_EXISTS, "C5Sub", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
