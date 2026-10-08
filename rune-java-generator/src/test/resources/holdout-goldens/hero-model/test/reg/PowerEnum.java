package test.reg;

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @version test
 */
@RosettaEnum("PowerEnum")
public enum PowerEnum {

	@RosettaEnumValue(value = "Armour") 
	ARMOUR("Armour", null),
	
	@RosettaEnumValue(value = "Flight") 
	FLIGHT("Flight", null),
	
	@RosettaEnumValue(value = "SuperhumanReflexes") 
	SUPERHUMAN_REFLEXES("SuperhumanReflexes", null),
	
	@RosettaEnumValue(value = "SuperhumanStrength") 
	SUPERHUMAN_STRENGTH("SuperhumanStrength", null)
;
	private static Map<String, PowerEnum> values;
	static {
        Map<String, PowerEnum> map = new ConcurrentHashMap<>();
		for (PowerEnum instance : PowerEnum.values()) {
			map.put(instance.toDisplayString(), instance);
		}
		values = Collections.unmodifiableMap(map);
    }

	private final String rosettaName;
	private final String displayName;

	PowerEnum(String rosettaName, String displayName) {
		this.rosettaName = rosettaName;
		this.displayName = displayName;
	}

	public static PowerEnum fromDisplayName(String name) {
		PowerEnum value = values.get(name);
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
