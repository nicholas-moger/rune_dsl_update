package test.aliascondmeta.validation.exists;

import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import com.rosetta.model.metafields.FieldWithMetaInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import test.aliascondmeta.MetaHolder;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class MetaHolderOnlyExistsValidator implements ValidatorWithArg<MetaHolder, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends MetaHolder> ValidationResult<MetaHolder> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("schemed", ExistenceChecker.isSet((FieldWithMetaInteger) o.getSchemed()))
				.put("schemeds", ExistenceChecker.isSet((List<? extends FieldWithMetaInteger>) o.getSchemeds()))
				.put("plain", ExistenceChecker.isSet((Integer) o.getPlain()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("MetaHolder", ValidationResult.ValidationType.ONLY_EXISTS, "MetaHolder", path, "");
		}
		return failure("MetaHolder", ValidationResult.ValidationType.ONLY_EXISTS, "MetaHolder", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
