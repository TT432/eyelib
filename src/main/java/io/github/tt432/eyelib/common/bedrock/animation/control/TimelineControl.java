package io.github.tt432.eyelib.common.bedrock.animation.control;

import io.github.tt432.eyelib.common.bedrock.animation.pojo.SingleAnimation;
import io.github.tt432.eyelib.common.bedrock.animation.pojo.TimelineEffect;
import io.github.tt432.eyelib.common.bedrock.animation.pojo.Timestamp;
import io.github.tt432.eyelib.molang.MolangParser;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * @author DustW
 */
public class TimelineControl {
    private final Queue<Map.Entry<Timestamp, TimelineEffect>> timelineQueue = new LinkedList<>();

    public void init(SingleAnimation animation) {
        if (animation != null) {
            var map = animation.getTimeline();

            if (map != null)
                timelineQueue.addAll(map.entrySet());
        }
    }

    public void stop(double tick) {
        process(tick);
        timelineQueue.clear();
    }

    public void process(double tick) {
        Map.Entry<Timestamp, TimelineEffect> curr = timelineQueue.peek();

        if (curr != null && tick >= curr.getKey().getTick()) {
            curr.getValue().eval(MolangParser.scopeStack.last());
            timelineQueue.poll();
        }
    }
}
