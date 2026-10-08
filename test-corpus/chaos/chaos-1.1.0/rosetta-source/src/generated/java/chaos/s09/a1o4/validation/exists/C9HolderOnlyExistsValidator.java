package chaos.s09.a1o4.validation.exists;

import chaos.s09.a1o4.C9Holder;
import chaos.s09.a1o4.C9Keyed;
import chaos.s09.a1o4.C9Plain;
import chaos.s09.a1o4.metafields.ReferenceWithMetaC9Keyed;
import com.google.common.collect.ImmutableMap;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ExistenceChecker;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import com.rosetta.model.metafields.FieldWithMetaString;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;

public class C9HolderOnlyExistsValidator implements ValidatorWithArg<C9Holder, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C9Holder> ValidationResult<C9Holder> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("direct", ExistenceChecker.isSet((C9Keyed) o.getDirect()))
				.put("byRef", ExistenceChecker.isSet((ReferenceWithMetaC9Keyed) o.getByRef()))
				.put("byRefs", ExistenceChecker.isSet((List<? extends ReferenceWithMetaC9Keyed>) o.getByRefs()))
				.put("coded", ExistenceChecker.isSet((FieldWithMetaString) o.getCoded()))
				.put("codes", ExistenceChecker.isSet((List<? extends FieldWithMetaString>) o.getCodes()))
				.put("marked", ExistenceChecker.isSet((FieldWithMetaString) o.getMarked()))
				.put("plain", ExistenceChecker.isSet((C9Plain) o.getPlain()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C9Holder", ValidationResult.ValidationType.ONLY_EXISTS, "C9Holder", path, "");
		}
		return failure("C9Holder", ValidationResult.ValidationType.ONLY_EXISTS, "C9Holder", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
