package chaos.s30.a1o1.validation.exists;

import chaos.s30.a1o1.C30Part;
import chaos.s30.a1o1.C30Whole;
import chaos.s30.a1o1.metafields.ReferenceWithMetaC30Part;
import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import com.rosetta.model.metafields.FieldWithMetaString;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class C30WholeOnlyExistsValidator implements ValidatorWithArg<C30Whole, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C30Whole> ValidationResult<C30Whole> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("name", ExistenceChecker.isSet((String) o.getName()))
				.put("code", ExistenceChecker.isSet((FieldWithMetaString) o.getCode()))
				.put("scores", ExistenceChecker.isSet((List<BigDecimal>) o.getScores()))
				.put("part", ExistenceChecker.isSet((C30Part) o.getPart()))
				.put("partRef", ExistenceChecker.isSet((ReferenceWithMetaC30Part) o.getPartRef()))
				.put("parts", ExistenceChecker.isSet((List<? extends C30Part>) o.getParts()))
				.put("nested", ExistenceChecker.isSet((C30Whole) o.getNested()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C30Whole", ValidationResult.ValidationType.ONLY_EXISTS, "C30Whole", path, "");
		}
		return failure("C30Whole", ValidationResult.ValidationType.ONLY_EXISTS, "C30Whole", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
