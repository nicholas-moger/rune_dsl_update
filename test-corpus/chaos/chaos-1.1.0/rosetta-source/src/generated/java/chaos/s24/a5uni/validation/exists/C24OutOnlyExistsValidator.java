package chaos.s24.a5uni.validation.exists;

import chaos.s24.a5uni.C24Carrier;
import chaos.s24.a5uni.C24Out;
import chaos.s24.a5uni.metafields.ReferenceWithMetaC24Keyed;
import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import com.rosetta.model.metafields.FieldWithMetaVoid;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class C24OutOnlyExistsValidator implements ValidatorWithArg<C24Out, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C24Out> ValidationResult<C24Out> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("v", ExistenceChecker.isSet((Void) o.getV()))
				.put("vm", ExistenceChecker.isSet((FieldWithMetaVoid) o.getVm()))
				.put("vs", ExistenceChecker.isSet((List<Void>) o.getVs()))
				.put("kref", ExistenceChecker.isSet((ReferenceWithMetaC24Keyed) o.getKref()))
				.put("s", ExistenceChecker.isSet((String) o.getS()))
				.put("car", ExistenceChecker.isSet((C24Carrier) o.getCar()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C24Out", ValidationResult.ValidationType.ONLY_EXISTS, "C24Out", path, "");
		}
		return failure("C24Out", ValidationResult.ValidationType.ONLY_EXISTS, "C24Out", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
