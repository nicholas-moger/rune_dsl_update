package chaos.s17.a2qual;

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Second enum in the SAME scope re-using Buy (the E2 wrong-enum class).
 * @version 1.0.0
 */
@RosettaEnum("C17ActionEnum")
public enum C17ActionEnum {

	@RosettaEnumValue(value = "Buy") 
	BUY("Buy", null),
	
	@RosettaEnumValue(value = "Amend") 
	AMEND("Amend", null)
;
	private static Map<String, C17ActionEnum> values;
	static {
        Map<String, C17ActionEnum> map = new ConcurrentHashMap<>();
		for (C17ActionEnum instance : C17ActionEnum.values()) {
			map.put(instance.toDisplayString(), instance);
		}
		values = Collections.unmodifiableMap(map);
    }

	private final String rosettaName;
	private final String displayName;

	C17ActionEnum(String rosettaName, String displayName) {
		this.rosettaName = rosettaName;
		this.displayName = displayName;
	}

	public static C17ActionEnum fromDisplayName(String name) {
		C17ActionEnum value = values.get(name);
		if (value == null) {
			throw new IllegalArgumentException("No enum constant with display name \"" + name + "\".");
		}
		return value;
	}

	@Override
	public String toString() {
		return toDisplayString();
	}

	public String toDisplayString() {
		return displayName != null ?  displayName : rosettaName;
	}
}
