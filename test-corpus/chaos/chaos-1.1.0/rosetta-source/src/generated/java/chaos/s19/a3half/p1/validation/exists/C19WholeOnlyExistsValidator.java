package chaos.s19.a3half.p1.validation.exists;

import chaos.s19.a3half.p1.C19Part;
import chaos.s19.a3half.p1.C19Whole;
import chaos.s19.a3half.p1.metafields.ReferenceWithMetaC19Part;
import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import com.rosetta.model.metafields.FieldWithMetaString;
import com.rosetta.model.metafields.ReferenceWithMetaString;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class C19WholeOnlyExistsValidator implements ValidatorWithArg<C19Whole, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C19Whole> ValidationResult<C19Whole> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("name", ExistenceChecker.isSet((String) o.getName()))
				.put("opt", ExistenceChecker.isSet((BigDecimal) o.getOpt()))
				.put("scores", ExistenceChecker.isSet((List<BigDecimal>) o.getScores()))
				.put("part", ExistenceChecker.isSet((C19Part) o.getPart()))
				.put("partRef", ExistenceChecker.isSet((ReferenceWithMetaC19Part) o.getPartRef()))
				.put("parts", ExistenceChecker.isSet((List<? extends C19Part>) o.getParts()))
				.put("spot", ExistenceChecker.isSet((FieldWithMetaString) o.getSpot()))
				.put("ptr", ExistenceChecker.isSet((ReferenceWithMetaString) o.getPtr()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C19Whole", ValidationResult.ValidationType.ONLY_EXISTS, "C19Whole", path, "");
		}
		return failure("C19Whole", ValidationResult.ValidationType.ONLY_EXISTS, "C19Whole", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
