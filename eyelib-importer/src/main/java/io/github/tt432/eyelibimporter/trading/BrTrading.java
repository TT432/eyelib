package io.github.tt432.eyelibimporter.trading;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * Bedrock 交易表的数据模型，支持 trading/*.json 和 economy_trades/*.json。
 * @author TT432
 */
@NullMarked
public record BrTrading(List<BrTier> tiers) {
    public static final Codec<BrTrading> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            BrTier.CODEC.listOf().fieldOf("tiers").forGetter(BrTrading::tiers)
    ).apply(ins, BrTrading::new));

    /**
     * Either 统一两种 tier 格式：直接含 trades 的简单格式（left），及含 groups 的完整格式（right）。
     */
    @NullMarked
    public record BrTier(int totalExpRequired, Either<List<BrTrade>, List<BrGroup>> content) {
        static final Codec<BrTier> CODEC = Codec.either(
                SimpleTierRaw.CODEC,
                GroupTierRaw.CODEC
        ).xmap(
                either -> either.map(
                        st -> new BrTier(st.totalExpRequired(), Either.left(st.trades())),
                        gt -> new BrTier(gt.totalExpRequired(), Either.right(gt.groups()))
                ),
                tier -> tier.content().map(
                        trades -> Either.left(new SimpleTierRaw(tier.totalExpRequired(), trades)),
                        groups -> Either.right(new GroupTierRaw(tier.totalExpRequired(), groups))
                )
        );

        private record SimpleTierRaw(int totalExpRequired, List<BrTrade> trades) {
            static final Codec<SimpleTierRaw> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                    Codec.INT.optionalFieldOf("total_exp_required", 0).forGetter(SimpleTierRaw::totalExpRequired),
                    BrTrade.CODEC.listOf().fieldOf("trades").forGetter(SimpleTierRaw::trades)
            ).apply(ins, SimpleTierRaw::new));
        }

        private record GroupTierRaw(int totalExpRequired, List<BrGroup> groups) {
            static final Codec<GroupTierRaw> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                    Codec.INT.optionalFieldOf("total_exp_required", 0).forGetter(GroupTierRaw::totalExpRequired),
                    BrGroup.CODEC.listOf().fieldOf("groups").forGetter(GroupTierRaw::groups)
            ).apply(ins, GroupTierRaw::new));
        }
    }

    /**
     * @param numToSelect 从 trades 中选取的交易数量，0 表示全选
     */
    @NullMarked
    public record BrGroup(int numToSelect, List<BrTrade> trades) {
        static final Codec<BrGroup> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.INT.optionalFieldOf("num_to_select", 0).forGetter(BrGroup::numToSelect),
                BrTrade.CODEC.listOf().fieldOf("trades").forGetter(BrGroup::trades)
        ).apply(ins, BrGroup::new));
    }

    /**
     * @param wants 玩家需付出的物品列表
     * @param gives 玩家将获得的物品列表
     */
    @NullMarked
    public record BrTrade(List<BrTradeEntry> wants, List<BrTradeEntry> gives) {
        static final Codec<BrTrade> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                BrTradeEntry.CODEC.listOf().fieldOf("wants").forGetter(BrTrade::wants),
                BrTradeEntry.CODEC.listOf().fieldOf("gives").forGetter(BrTrade::gives)
        ).apply(ins, BrTrade::new));
    }

    /**
     * wants/gives 中的条目，可能为单件物品（left）或 choice 列表（right）。
     */
    @NullMarked
    public record BrTradeEntry(Either<BrTradeItem, List<BrTradeItem>> content) {
        static final Codec<BrTradeEntry> CODEC = Codec.either(
                BrTradeItem.CODEC,
                ChoiceWrapper.CODEC
        ).xmap(
                either -> either.map(
                        item -> new BrTradeEntry(Either.left(item)),
                        cw -> new BrTradeEntry(Either.right(cw.choice()))
                ),
                entry -> entry.content().map(
                        item -> Either.left(item),
                        items -> Either.right(new ChoiceWrapper(items))
                )
        );

        private record ChoiceWrapper(List<BrTradeItem> choice) {
            static final Codec<ChoiceWrapper> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                    BrTradeItem.CODEC.listOf().fieldOf("choice").forGetter(ChoiceWrapper::choice)
            ).apply(ins, ChoiceWrapper::new));
        }
    }

    /**
     * @param quantity 物品数量，未指定时默认为 1
     */
    @NullMarked
    public record BrTradeItem(String item, BrQuantity quantity) {
        static final Codec<BrTradeItem> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.STRING.fieldOf("item").forGetter(BrTradeItem::item),
                BrQuantity.CODEC.optionalFieldOf("quantity", new BrQuantity(1, 1)).forGetter(BrTradeItem::quantity)
        ).apply(ins, BrTradeItem::new));
    }

    /**
     * 物品数量，支持单值和 {min,max} 区间。单值解码后 min == max。
     */
    @NullMarked
    public record BrQuantity(int min, int max) {
        static final Codec<BrQuantity> CODEC = Codec.either(
                Codec.INT,
                QuantityRaw.CODEC
        ).xmap(
                either -> either.map(
                        i -> new BrQuantity(i, i),
                        qr -> new BrQuantity(qr.min(), qr.max())
                ),
                q -> q.min() == q.max()
                        ? Either.left(q.min())
                        : Either.right(new QuantityRaw(q.min(), q.max()))
        );

        private record QuantityRaw(int min, int max) {
            static final Codec<QuantityRaw> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                    Codec.INT.fieldOf("min").forGetter(QuantityRaw::min),
                    Codec.INT.fieldOf("max").forGetter(QuantityRaw::max)
            ).apply(ins, QuantityRaw::new));
        }
    }
}
