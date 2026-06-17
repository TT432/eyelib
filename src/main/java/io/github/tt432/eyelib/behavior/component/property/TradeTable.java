package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * minecraft:trade_table
 *
 * @param display_name display name (default "")
 * @param table        trade table path
 * @param trades       list of trades
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record TradeTable(
        String display_name,
        String table,
        List<Trade> trades
) implements io.github.tt432.eyelib.behavior.component.Component {
    public static final Codec<TradeTable> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.optionalFieldOf("display_name", "").forGetter(TradeTable::display_name),
            Codec.STRING.fieldOf("table").forGetter(TradeTable::table),
            Trade.CODEC.listOf().fieldOf("trades").forGetter(TradeTable::trades)
    ).apply(inst, TradeTable::new));

    @Override
    public String id() {
        return "trade_table";
    }

    public record Trade(
            List<TradeItem> wants,
            List<TradeItem> gives,
            int max_uses,
            boolean reward_exp
    ) {
        static final Codec<Trade> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                TradeItem.CODEC.listOf().fieldOf("wants").forGetter(Trade::wants),
                TradeItem.CODEC.listOf().fieldOf("gives").forGetter(Trade::gives),
                Codec.INT.fieldOf("max_uses").forGetter(Trade::max_uses),
                Codec.BOOL.optionalFieldOf("reward_exp", false).forGetter(Trade::reward_exp)
        ).apply(inst, Trade::new));
    }

    public record TradeItem(String item, int quantity) {
        static final Codec<TradeItem> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.fieldOf("item").forGetter(TradeItem::item),
                Codec.INT.optionalFieldOf("quantity", 1).forGetter(TradeItem::quantity)
        ).apply(inst, TradeItem::new));
    }
}
