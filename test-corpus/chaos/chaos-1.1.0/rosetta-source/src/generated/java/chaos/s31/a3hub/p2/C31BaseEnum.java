package chaos.s31.a3hub.p2;

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Extends chain 1.
 * @version 1.0.0
 */
@RosettaEnum("C31BaseEnum")
public enum C31BaseEnum {

	@RosettaEnumValue(value = "A") 
	A("A", null)
;
	private static Map<String, C31BaseEnum> values;
	static {
        Map<String, C31BaseEnum> map = new ConcurrentHashMap<>();
		for (C31BaseEnum instance : C31BaseEnum.values()) {
			map.put(instance.toDisplayString(), instance);
		}
		values = Collections.unmodifiableMap(map);
    }

	private final String rosettaName;
	private final String displayName;

	C31BaseEnum(String rosettaName, String displayName) {
		this.rosettaName = rosettaName;
		this.displayName = displayName;
	}

	public static C31BaseEnum fromDisplayName(String name) {
		C31BaseEnum value = values.get(name);
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
