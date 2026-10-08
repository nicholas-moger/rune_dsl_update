package chaos.s17.a6enum.rival;

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * RIVAL - a same-named enum in an imported namespace whose Buy is the WRONG Buy.
 * @version 1.0.0
 */
@RosettaEnum("C17SideEnum")
public enum C17SideEnum {

	@RosettaEnumValue(value = "Buy") 
	BUY("Buy", null),
	
	@RosettaEnumValue(value = "Hold") 
	HOLD("Hold", null)
;
	private static Map<String, C17SideEnum> values;
	static {
        Map<String, C17SideEnum> map = new ConcurrentHashMap<>();
		for (C17SideEnum instance : C17SideEnum.values()) {
			map.put(instance.toDisplayString(), instance);
		}
		values = Collections.unmodifiableMap(map);
    }

	private final String rosettaName;
	private final String displayName;

	C17SideEnum(String rosettaName, String displayName) {
		this.rosettaName = rosettaName;
		this.displayName = displayName;
	}

	public static C17SideEnum fromDisplayName(String name) {
		C17SideEnum value = values.get(name);
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
