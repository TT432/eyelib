package io.github.tt432.eyelib.common.behavior.event.logic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.capability.EntityBehaviorData;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author TT432
 */
public record Randomize(
        List<Entry> entries
) implements LogicNode {
    public static final Codec<Randomize> CODEC = Entry.CODEC.listOf().xmap(Randomize::new, Randomize::entries);

    public void eval(EntityBehaviorData data) {
        List<Entry> entries = this.entries;
        // 提前进行空列表检查，避免后续不必要的计算
        if (entries.isEmpty()) {
            return;
        }

        int totalWeight = 0;
        // 预计算权重数组，方便后续查找
        int[] prefixSums = new int[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            totalWeight += entry.weight();
            prefixSums[i] = totalWeight;
        }

        // 防御0权重总和
        if (totalWeight == 0) {
            return;
        }

        // 生成[0, totalWeight)范围内的随机数
        int random = ThreadLocalRandom.current().nextInt(totalWeight);

        // 使用二分查找优化轮盘赌选择
        int left = 0, right = prefixSums.length - 1;
        while (left < right) {
            int mid = left + (right - left) / 2;
            if (random >= prefixSums[mid]) {
                left = mid + 1;
            } else {
                right = mid;
            }
        }

        // 执行选中节点
        entries.get(left).node().eval(data);
    }

    public record Entry(
            int weight,
            LogicNode node
    ) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.INT.fieldOf("weight").forGetter(Entry::weight),
                LogicNode.CODEC.forGetter(Entry::node)
        ).apply(ins, Entry::new));
    }
}
