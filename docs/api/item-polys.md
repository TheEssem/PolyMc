---
layout: default
title: Item Polys
nav_order: 11
---

# Item Polys
At a basic level item polys take in the servers sided item and output one to be displayed on the client.
The default item poly, the `CustomModelDataPoly`, works quite well in most cases.
All of PolyMc's item polys are based on the `CustomModelDataPoly`. The biggest reason for making a custom item poly is to customize the resources that get stored for that poly.
Keep in mind that, if you create a custom item poly, it should never modify the input itemstack directly. Use `copy()` instead.

## Example: changing the item used on the client
```java
public void registerPolys(PolyRegistry registry) {
    registry.registerItemPoly(moddedItem, new CustomModelDataPoly(registery.getCMDManager(), moddedItem, Items.DIAMOND_SWORD))
}
```
*note: if your modded item is damageable, replace `CustomModelDataPoly` with `DamageableItemPoly`*  
This will Poly your item as usual, just using a diamond sword instead of a stick. It will automatically allocate a CMD value for it.

## Global item polys
Global item polys are applied to all items before they are sent to the client. Global item polys implement `ItemTransformer` and can be registered using `PolyRegistry#registerGlobalItemPoly`.
Keep in mind that, like normal item polys, global item polys should *never* modify the input itemstack directly.

# The CustomModelDataManager

The CMD manager is made to prevent polys from using the same CMD values on the same items.
At the simplest level, you can request a CMD value to use for a specific item.
You can also request a number of values, the number returned is the first value you may use, see the javadoc for more detail.
The CMD manager can also round-robin from a list of items.
This is the preferred way of doing things as this prevents recipe conflicts (the recipe book doesn't take the CMD value into account, this isn't a huge deal but is a minor annoyance to clients).

