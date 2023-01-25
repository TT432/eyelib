package software.bernie.geckolib3.geo.raw.pojo;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class RawGeoModel {
	@SerializedName("format_version")
	private FormatVersion formatVersion;
	@SerializedName("minecraft:geometry")
	private MinecraftGeometry[] minecraftGeometry;
}
