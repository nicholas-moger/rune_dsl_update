package chaos.s01.a1o4.validation.exists;

import chaos.s01.a1o4.C1Mid;
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

public class C1MidOnlyExistsValidator implements ValidatorWithArg<C1Mid, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C1Mid> ValidationResult<C1Mid> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("baseId", ExistenceChecker.isSet((String) o.getBaseId()))
				.put("note", ExistenceChecker.isSet((String) o.getNote()))
				.put("mids", ExistenceChecker.isSet((List<BigDecimal>) o.getMids()))
				.put("asOf", ExistenceChecker.isSet((Date) o.getAsOf()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C1Mid", ValidationResult.ValidationType.ONLY_EXISTS, "C1Mid", path, "");
		}
		return failure("C1Mid", ValidationResult.ValidationType.ONLY_EXISTS, "C1Mid", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
