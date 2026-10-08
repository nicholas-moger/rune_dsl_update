package chaos.s31.a1o1;

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Extends chain 2.
 * @version 1.0.0
 */
@RosettaEnum("C31MidEnum")
public enum C31MidEnum {

	@RosettaEnumValue(value = "A") 
	A("A", null),
	
	@RosettaEnumValue(value = "B") 
	B("B", null)
;
	private static Map<String, C31MidEnum> values;
	static {
        Map<String, C31MidEnum> map = new ConcurrentHashMap<>();
		for (C31MidEnum instance : C31MidEnum.values()) {
			map.put(instance.toDisplayString(), instance);
		}
		values = Collections.unmodifiableMap(map);
    }

	private final String rosettaName;
	private final String displayName;

	C31MidEnum(String rosettaName, String displayName) {
		this.rosettaName = rosettaName;
		this.displayName = displayName;
	}

	public static C31MidEnum fromDisplayName(String name) {
		C31MidEnum value = values.get(name);
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
