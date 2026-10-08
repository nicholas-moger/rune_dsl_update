package chaos.s02.a1o1;

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Extension base.
 * @version 1.0.0
 */
@RosettaEnum("C2BaseEnum")
public enum C2BaseEnum {

	@RosettaEnumValue(value = "Alpha") 
	ALPHA("Alpha", null),
	
	@RosettaEnumValue(value = "Beta", displayName = "B") 
	BETA("Beta", "B")
;
	private static Map<String, C2BaseEnum> values;
	static {
        Map<String, C2BaseEnum> map = new ConcurrentHashMap<>();
		for (C2BaseEnum instance : C2BaseEnum.values()) {
			map.put(instance.toDisplayString(), instance);
		}
		values = Collections.unmodifiableMap(map);
    }

	private final String rosettaName;
	private final String displayName;

	C2BaseEnum(String rosettaName, String displayName) {
		this.rosettaName = rosettaName;
		this.displayName = displayName;
	}

	public static C2BaseEnum fromDisplayName(String name) {
		C2BaseEnum value = values.get(name);
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
