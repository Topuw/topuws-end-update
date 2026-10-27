
package com.topuw.end;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public final class ModItems {

    public static final ResourceKey<Item> ENDERMITE_BUCKET_KEY =
            ResourceKey.create(
                    Registries.ITEM,
                    Identifier.fromNamespaceAndPath(
                            TopuwSEndUpdate.MOD_ID,
                            "endermite_bucket"
                    )
            );

    public static final Item ENDERMITE_BUCKET =
            Registry.register(
                    BuiltInRegistries.ITEM,
                    ENDERMITE_BUCKET_KEY,
                    new Item(
                            new Item.Properties()
                                    .setId(ENDERMITE_BUCKET_KEY)
                                    .stacksTo(1)
                    )
            );

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)
                .register(entries -> entries.accept(ENDERMITE_BUCKET));
    }

    private ModItems() {
    }
}
