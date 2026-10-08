package chaos.s11.a2wild.validation.exists;

import chaos.s11.a2wild.C11Mapped;
import chaos.s11.a2wild.h.C11Aux;
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

public class C11MappedOnlyExistsValidator implements ValidatorWithArg<C11Mapped, Set<String>> {

	/* Casting is required to ensure types are output to ensure recompilation in Rosetta */
	@Override
	public <T2 extends C11Mapped> ValidationResult<C11Mapped> validate(RosettaPath path, T2 o, Set<String> fields) {
		Map<String, Boolean> fieldExistenceMap = ImmutableMap.<String, Boolean>builder()
				.put("ident", ExistenceChecker.isSet((String) o.getIdent()))
				.put("total", ExistenceChecker.isSet((BigDecimal) o.getTotal()))
				.put("kindCode", ExistenceChecker.isSet((String) o.getKindCode()))
				.put("flagged", ExistenceChecker.isSet((String) o.getFlagged()))
				.put("merged", ExistenceChecker.isSet((List<String>) o.getMerged()))
				.put("tagged", ExistenceChecker.isSet((String) o.getTagged()))
				.put("metaCarrier", ExistenceChecker.isSet((String) o.getMetaCarrier()))
				.put("defaulted", ExistenceChecker.isSet((String) o.getDefaulted()))
				.put("tested", ExistenceChecker.isSet((String) o.getTested()))
				.put("pathed", ExistenceChecker.isSet((String) o.getPathed()))
				.put("dated", ExistenceChecker.isSet((Date) o.getDated()))
				.put("patterned", ExistenceChecker.isSet((String) o.getPatterned()))
				.put("metaOnly", ExistenceChecker.isSet((String) o.getMetaOnly()))
				.put("aux", ExistenceChecker.isSet((C11Aux) o.getAux()))
				.build();
		
		// Find the fields that are set
		Set<String> setFields = fieldExistenceMap.entrySet().stream()
				.filter(Map.Entry::getValue)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		
		if (setFields.equals(fields)) {
			return success("C11Mapped", ValidationResult.ValidationType.ONLY_EXISTS, "C11Mapped", path, "");
		}
		return failure("C11Mapped", ValidationResult.ValidationType.ONLY_EXISTS, "C11Mapped", path, "",
				String.format("[%s] should only be set.  Set fields: %s", fields, setFields));
	}
}
