package io.github.tt432.eyelib.behavior.event.logic;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.behavior.EntityBehaviorData;
import io.github.tt432.eyelib.behavior.event.filter.Subject;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

/**
 * queue_command 事件节点，将命令加入执行队列。
 * <p>
 * Bedrock 标准支持 command 为单字符串或字符串数组两种形式：
 * <ul>
 *   <li>{@code "command": "summon pig"}</li>
 *   <li>{@code "command": ["summon pig", "say hello"]}</li>
 * </ul>
 *
 * @param target  目标主体（self / other 等），默认 self
 * @param command 要执行的命令列表（单字符串或字符串数组）
 * @author TT432
 */
public record QueueCommand(
        @Nullable Subject target,
        List<String> command
) implements LogicNode {
    public static final Codec<QueueCommand> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Subject.CODEC.optionalFieldOf("target", Subject.self).forGetter(QueueCommand::target),
            Codec.either(Codec.STRING, Codec.STRING.listOf())
                    .xmap(
                            e -> e.map(List::of, Function.identity()),
                            l -> l.size() == 1 ? Either.left(l.get(0)) : Either.right(l)
                    ).fieldOf("command").forGetter(QueueCommand::command)
    ).apply(ins, QueueCommand::new));

    @Override
    public void eval(EntityBehaviorData data) {
        // Phase 1 实现: 命令执行器接口占位
        // TODO: 对接实际命令执行系统
        // for (String cmd : command) {
        //     CommandQueue.enqueue(target, cmd);
        // }
    }
}
