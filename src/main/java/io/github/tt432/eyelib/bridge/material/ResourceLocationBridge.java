package io.github.tt432.eyelib.bridge.material;

import io.github.tt432.eyelib.util.PortResourceLocation;
//? if <26.1 {
import net.minecraft.resources.ResourceLocation;
//?} else {
import net.minecraft.resources.Identifier;
//?}
/**
 * PortResourceLocation ↔ MC ResourceLocation 的双向桥接。
 *
 * @author TT432
 */
public final class ResourceLocationBridge {

    private ResourceLocationBridge() {}

    //? if <26.1 {
    public static ResourceLocation toMc(PortResourceLocation port) {
    //?} else {
    public static Identifier toMc(PortResourceLocation port) {
    //?}
        //? if <1.20.6 {
        return new ResourceLocation(port.namespace(), port.path());
        //?} elif <26.1 {
        return ResourceLocation.fromNamespaceAndPath(port.namespace(), port.path());
        //?} else {
        return Identifier.fromNamespaceAndPath(port.namespace(), port.path());

        //?}
    }

    //? if <26.1 {
    public static PortResourceLocation fromMc(ResourceLocation mc) {
    //?} else {
    public static PortResourceLocation fromMc(Identifier mc) {
    //?}
        return PortResourceLocation.of(mc.getNamespace(), mc.getPath());
    }

    //? if <26.1 {
    public static ResourceLocation parseMc(String value) {
    //?} else {
    public static Identifier parseMc(String value) {
    //?}
        //? if <1.20.6 {
        return new ResourceLocation(value);
        //?} elif <26.1 {
        return ResourceLocation.parse(value);
        //?} else {
        return Identifier.parse(value);

        //?}
    }

    //? if <26.1 {
    public static ResourceLocation fromParts(String namespace, String path) {
    //?} else {
    public static Identifier fromParts(String namespace, String path) {
    //?}
        //? if <1.20.6 {
        return new ResourceLocation(namespace, path);
        //?} elif <26.1 {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
        //?} else {
        return Identifier.fromNamespaceAndPath(namespace, path);

        //?}
    }
}
