package io.github.tt432.eyelib.client.model.bbmodel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.util.codec.EyelibCodec;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author TT432
 */
@SuppressWarnings("NullAway")
public record Outliner(
        String uuid,
        Optional<Group> group,
        boolean isOpen,
        List<String> cubes,
        List<Outliner> children
) {
    public record CubeOrOutliner(
            @Nullable Outliner outliner,
            @Nullable String uuid
    ) {
    }

    public static final Codec<Outliner> CODEC = EyelibCodec.recursive("Outliner", self ->
            RecordCodecBuilder.create(ins -> ins.group(
                    Codec.STRING.fieldOf("uuid").forGetter(Outliner::uuid),
                    EyelibCodec.optionalMapCodec(Group.MAP_CODEC).forGetter(Outliner::group),
                    Codec.BOOL.fieldOf("isOpen").forGetter(Outliner::isOpen),
                    EyelibCodec.withAlternative(
                                    self.xmap(
                                            o -> new CubeOrOutliner(o, null),
                                            c -> c.outliner != null ? c.outliner : null
                                    ),
                                    Codec.STRING.xmap(
                                            o -> new CubeOrOutliner(null, o),
                                            c -> c.uuid != null ? c.uuid : null
                                    )
                            )
                            .listOf().fieldOf("children").forGetter(o -> {
                                List<CubeOrOutliner> result = new ArrayList<>();
                                o.children.forEach(oo -> result.add(new CubeOrOutliner(oo, null)));
                                o.cubes.forEach(oo -> result.add(new CubeOrOutliner(null, oo)));
                                return result;
                            })
            ).apply(ins, (uuid, group, isOpen, children) -> new Outliner(
                    uuid,
                    group,
                    isOpen,
                    children.stream().filter(c -> c.uuid != null).map(CubeOrOutliner::uuid).toList(),
                    children.stream().filter(c -> c.outliner != null).map(CubeOrOutliner::outliner).toList()
            ))));
}
