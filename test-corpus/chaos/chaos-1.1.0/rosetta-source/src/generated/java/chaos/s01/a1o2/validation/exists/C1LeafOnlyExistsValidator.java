package chaos.s01.a1o2.validation.exists;

import chaos.s01.a1o2.C1Leaf;
import chaos.s01.a1o2.C1Ref;
import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.records.Date;
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

public class C1LeafOnlyExistsValidator implements ValidatorWithArg<C1Leaf, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C1Leaf> ValidationResult<C1Leaf> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("baseId", ExistenceChecker.isSet((String) o.getBaseId()))
				.put("note", ExistenceChecker.isSet((String) o.getNote()))
				.put("mids", ExistenceChecker.isSet((List<BigDecimal>) o.getMids()))
				.put("asOf", ExistenceChecker.isSet((Date) o.getAsOf()))
				.put("parts", ExistenceChecker.isSet((List<? extends C1Ref>) o.getParts()))
				.put("ratio", ExistenceChecker.isSet((BigDecimal) o.getRatio()))
				.put("code", ExistenceChecker.isSet((String) o.getCode()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C1Leaf", ValidationResult.ValidationType.ONLY_EXISTS, "C1Leaf", path, "");
		}
		return failure("C1Leaf", ValidationResult.ValidationType.ONLY_EXISTS, "C1Leaf", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
