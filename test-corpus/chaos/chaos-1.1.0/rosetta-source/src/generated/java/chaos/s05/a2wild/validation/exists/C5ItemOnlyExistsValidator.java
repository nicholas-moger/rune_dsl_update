package chaos.s05.a2wild.validation.exists;

import chaos.s05.a2wild.C5Item;
import chaos.s05.a2wild.h.C5Sub;
import chaos.s05.a2wild.h.metafields.ReferenceWithMetaC5Sub;
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

public class C5ItemOnlyExistsValidator implements ValidatorWithArg<C5Item, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C5Item> ValidationResult<C5Item> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("one", ExistenceChecker.isSet((String) o.getOne()))
				.put("opt", ExistenceChecker.isSet((BigDecimal) o.getOpt()))
				.put("many", ExistenceChecker.isSet((List<BigDecimal>) o.getMany()))
				.put("sub", ExistenceChecker.isSet((List<? extends C5Sub>) o.getSub()))
				.put("subRef", ExistenceChecker.isSet((ReferenceWithMetaC5Sub) o.getSubRef()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C5Item", ValidationResult.ValidationType.ONLY_EXISTS, "C5Item", path, "");
		}
		return failure("C5Item", ValidationResult.ValidationType.ONLY_EXISTS, "C5Item", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
