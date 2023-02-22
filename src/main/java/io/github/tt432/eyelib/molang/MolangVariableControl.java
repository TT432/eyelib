package io.github.tt432.eyelib.molang;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

/**
 * @author DustW
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MolangVariableControl {
    public static void living(MolangVariableScope scope) {
        scope.setVariable("query.has_helmet", s -> livingBool(s, living -> !living.getItemBySlot(EquipmentSlot.HEAD).isEmpty()));
        scope.setVariable("query.has_chestplate", s -> livingBool(s, living -> !living.getItemBySlot(EquipmentSlot.CHEST).isEmpty()));
        scope.setVariable("query.has_leggings", s -> livingBool(s, living -> !living.getItemBySlot(EquipmentSlot.LEGS).isEmpty()));
        scope.setVariable("query.has_boots", s -> livingBool(s, living -> !living.getItemBySlot(EquipmentSlot.FEET).isEmpty()));
    }

    static Double livingDouble(MolangVariableScope scope, ToDoubleFunction<LivingEntity> func) {
        LivingEntity livingEntity = scope.getDataSource().get(LivingEntity.class);

        if (livingEntity != null) {
            return func.applyAsDouble(livingEntity);
        }

        return 0.;
    }

    static Double livingBool(MolangVariableScope scope, Predicate<LivingEntity> predicate) {
        return livingDouble(scope, living -> predicate.test(living) ? MolangValue.TRUE : MolangValue.FALSE);
    }
}
