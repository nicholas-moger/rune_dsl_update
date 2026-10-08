package chaos.s31.a1o1;

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * The second enum in the SAME scope re-using Long.
 * @version 1.0.0
 */
@RosettaEnum("C31MoveEnum")
public enum C31MoveEnum {

	@RosettaEnumValue(value = "Long") 
	LONG("Long", null),
	
	@RosettaEnumValue(value = "Flat") 
	FLAT("Flat", null)
;
	private static Map<String, C31MoveEnum> values;
	static {
        Map<String, C31MoveEnum> map = new ConcurrentHashMap<>();
		for (C31MoveEnum instance : C31MoveEnum.values()) {
			map.put(instance.toDisplayString(), instance);
		}
		values = Collections.unmodifiableMap(map);
    }

	private final String rosettaName;
	private final String displayName;

	C31MoveEnum(String rosettaName, String displayName) {
		this.rosettaName = rosettaName;
		this.displayName = displayName;
	}

	public static C31MoveEnum fromDisplayName(String name) {
		C31MoveEnum value = values.get(name);
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
