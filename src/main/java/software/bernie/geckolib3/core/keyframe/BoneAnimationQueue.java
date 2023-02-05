/*
 * Copyright (c) 2020.
 * Author: Bernie G. (Gecko)
 */

package software.bernie.geckolib3.core.keyframe;

import io.github.tt432.eyelib.api.model.Bone;

public record BoneAnimationQueue(Bone bone,
								 AnimationPointQueue rotate,
								 AnimationPointQueue position,
								 AnimationPointQueue scale) {

	public BoneAnimationQueue(Bone bone) {
		this(bone, new AnimationPointQueue(), new AnimationPointQueue(), new AnimationPointQueue());
	}
}
