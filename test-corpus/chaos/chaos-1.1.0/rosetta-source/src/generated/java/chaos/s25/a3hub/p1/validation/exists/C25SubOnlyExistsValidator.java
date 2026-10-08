package chaos.s25.a3hub.p1.validation.exists;

import chaos.s25.a3hub.p1.C25Sub;
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

public class C25SubOnlyExistsValidator implements ValidatorWithArg<C25Sub, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C25Sub> ValidationResult<C25Sub> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("sname", ExistenceChecker.isSet((String) o.getSname()))
				.put("subs", ExistenceChecker.isSet((List<BigDecimal>) o.getSubs()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C25Sub", ValidationResult.ValidationType.ONLY_EXISTS, "C25Sub", path, "");
		}
		return failure("C25Sub", ValidationResult.ValidationType.ONLY_EXISTS, "C25Sub", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
