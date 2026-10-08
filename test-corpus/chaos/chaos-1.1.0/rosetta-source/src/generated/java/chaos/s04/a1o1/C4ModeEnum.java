package chaos.s04.a1o1;

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Dispatch discriminator.
 * @version 1.0.0
 */
@RosettaEnum("C4ModeEnum")
public enum C4ModeEnum {

	@RosettaEnumValue(value = "Fast") 
	FAST("Fast", null),
	
	@RosettaEnumValue(value = "Slow") 
	SLOW("Slow", null)
;
	private static Map<String, C4ModeEnum> values;
	static {
        Map<String, C4ModeEnum> map = new ConcurrentHashMap<>();
		for (C4ModeEnum instance : C4ModeEnum.values()) {
			map.put(instance.toDisplayString(), instance);
		}
		values = Collections.unmodifiableMap(map);
    }

	private final String rosettaName;
	private final String displayName;

	C4ModeEnum(String rosettaName, String displayName) {
		this.rosettaName = rosettaName;
		this.displayName = displayName;
	}

	public static C4ModeEnum fromDisplayName(String name) {
		C4ModeEnum value = values.get(name);
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
