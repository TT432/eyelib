package io.github.tt432.eyelib.client.model.flat;

import io.github.tt432.eyelib.client.model.bedrock.BrBone;
import io.github.tt432.eyelib.client.model.bedrock.BrCube;
import io.github.tt432.eyelib.client.model.bedrock.BrModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author TT432
 */
public record FlatBrModel(
        BrModel rawModel,
        FlatBrModelCommand[] commands
) {

    public FlatBrModel copy() {
        return FlatBrModel.bake(rawModel.copy());
    }

    public static FlatBrModel bake(BrModel model) {
        List<FlatBrModelCommand> commands = new ArrayList<>();

        List<BrBone> toplevelBones = model.toplevelBones();

        for (BrBone toplevelBone : toplevelBones) {
            bakeBone(commands, toplevelBone);
        }

        return new FlatBrModel(model, commands.toArray(new FlatBrModelCommand[0]));
    }

    static void bakeBone(List<FlatBrModelCommand> commands, BrBone bone) {
        commands.add(new EnterBoneCommand(bone));

        for (BrCube cube : bone.getCubes()) {
            commands.add(new FillCubeVertexCommand(cube));
        }

        for (BrBone child : bone.getChildren()) {
            bakeBone(commands, child);
        }

        commands.add(new ExitBoneCommand());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof FlatBrModel fbm && fbm.rawModel.equals(rawModel) && Arrays.equals(commands, fbm.commands);
    }

    @Override
    public int hashCode() {
        return rawModel.hashCode() & Arrays.hashCode(commands);
    }

    @Override
    public String toString() {
        return "model: {" +
                rawModel.toString() +
                "};" +
                "commands: {" +
                Arrays.toString(commands) +
                "}";
    }
}
