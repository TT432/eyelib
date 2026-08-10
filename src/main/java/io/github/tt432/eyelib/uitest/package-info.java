/**
 * LDLib2 in-client UI 测试场景（uitest 框架，规格见 gradle/ldlib2-uitest.gradle）。
 *
 * <p>场景经 {@code @LDLRegisterClient(registry = UIScenario.REGISTRY)} 注解扫描注册，
 * DEV_ONLY 环境——只在 dev 客户端装配。26.1.2.33 尚无 uitest 框架，场景类按版本守卫排除。
 */
@NullMarked
package io.github.tt432.eyelib.uitest;

import org.jspecify.annotations.NullMarked;
