package chaos.s24.a2dangle.validation.exists;

import chaos.s24.a2dangle.C24Carrier;
import chaos.s24.a2dangle.h.C24Ref;
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

public class C24CarrierOnlyExistsValidator implements ValidatorWithArg<C24Carrier, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C24Carrier> ValidationResult<C24Carrier> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("tok", ExistenceChecker.isSet((Void) o.getTok()))
				.put("toks", ExistenceChecker.isSet((List<Void>) o.getToks()))
				.put("coded", ExistenceChecker.isSet((FieldWithMetaVoid) o.getCoded()))
				.put("name", ExistenceChecker.isSet((String) o.getName()))
				.put("flag", ExistenceChecker.isSet((Boolean) o.getFlag()))
				.put("ref", ExistenceChecker.isSet((C24Ref) o.getRef()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C24Carrier", ValidationResult.ValidationType.ONLY_EXISTS, "C24Carrier", path, "");
		}
		return failure("C24Carrier", ValidationResult.ValidationType.ONLY_EXISTS, "C24Carrier", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
