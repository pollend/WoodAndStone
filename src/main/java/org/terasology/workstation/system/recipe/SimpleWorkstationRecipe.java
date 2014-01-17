/*
 * Copyright 2014 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.workstation.system.recipe;

import org.terasology.durability.DurabilityComponent;
import org.terasology.durability.ReduceDurabilityEvent;
import org.terasology.engine.CoreRegistry;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.logic.inventory.SlotBasedInventoryManager;
import org.terasology.workstation.component.CraftingStationIngredientComponent;
import org.terasology.world.block.BlockManager;
import org.terasology.world.block.entity.BlockDamageComponent;
import org.terasology.world.block.items.BlockItemFactory;

import java.util.*;

/**
 * @author Marcin Sciesinski <marcins78@gmail.com>
 */
public class SimpleWorkstationRecipe implements CraftingStationRecipe {
    private Map<String, Integer> ingredientsMap = new LinkedHashMap<>();
    private Map<String, Integer> toolsMap = new LinkedHashMap<>();

    private String blockResult;
    private String itemResult;
    private byte count;

    private SlotBasedInventoryManager inventoryManager = CoreRegistry.get(SlotBasedInventoryManager.class);

    public void addIngredient(String type, int count) {
        ingredientsMap.put(type, count);
    }

    public void addRequiredTool(String toolType, int durability) {
        toolsMap.put(toolType, durability);
    }

    public void setBlockResult(String blockResult, byte count) {
        this.blockResult = blockResult;
        this.count = count;
    }

    public void setItemResult(String itemResult, byte count) {
        this.itemResult = itemResult;
        this.count = count;
    }

    @Override
    public boolean hasAsComponent(EntityRef itemEntity) {
        CraftingStationIngredientComponent ingredient = itemEntity.getComponent(CraftingStationIngredientComponent.class);
        return ingredient != null && ingredientsMap.containsKey(ingredient.type);
    }

    @Override
    public boolean hasAsTool(EntityRef itemEntity) {
        ItemComponent component = itemEntity.getComponent(ItemComponent.class);
        if (component == null)
            return false;
        BlockDamageComponent blockDamage = component.damageType.getComponent(BlockDamageComponent.class);
        if (blockDamage == null)
            return false;
        for (String tool : toolsMap.keySet()) {
            if (blockDamage.materialDamageMultiplier.containsKey(tool))
                return true;
        }
        return false;
    }

    @Override
    public List<CraftingStationResult> getMatchingRecipeResults(EntityRef station, int componentFromSlot, int componentSlotCount, int toolFromSlot, int toolSlotCount) {
        // TODO: Improve the search to find fragmented ingredients in multiple stacks, and also to find different kinds
        // of items, not just first matching
        List<Integer> resultSlots = new LinkedList<>();
        for (Map.Entry<String, Integer> ingredientCount : ingredientsMap.entrySet()) {
            int slotNo = hasItem(station, ingredientCount.getKey(), ingredientCount.getValue(), componentFromSlot, componentFromSlot + componentSlotCount);
            if (slotNo != -1)
                resultSlots.add(slotNo);
            else
                return null;
        }

        for (Map.Entry<String, Integer> toolDurability : toolsMap.entrySet()) {
            int slotNo = hasTool(station, toolDurability.getKey(), toolDurability.getValue(), toolFromSlot, toolFromSlot + toolSlotCount);
            if (slotNo != -1)
                resultSlots.add(slotNo);
            else
                return null;
        }

        return Collections.<CraftingStationResult>singletonList(new Result(resultSlots));
    }

    private int hasTool(EntityRef station, String toolType, int durability, int fromSlot, int toSlot) {
        for (int i = fromSlot; i < toSlot; i++) {
            if (hasToolInSlot(station, toolType, i, durability))
                return i;
        }

        return -1;
    }

    private int hasItem(EntityRef station, String itemType, int count, int fromSlot, int toSlot) {
        for (int i = fromSlot; i < toSlot; i++) {
            if (hasItemInSlot(station, itemType, i, count))
                return i;
        }

        return -1;
    }

    private boolean hasItemInSlot(EntityRef station, String itemType, int slot, int count) {
        EntityRef item = inventoryManager.getItemInSlot(station, slot);
        CraftingStationIngredientComponent component = item.getComponent(CraftingStationIngredientComponent.class);
        if (component != null && component.type.equals(itemType) && item.getComponent(ItemComponent.class).stackCount >= count)
            return true;
        return false;
    }

    private boolean hasToolInSlot(EntityRef station, String toolType, int slot, int durability) {
        EntityRef item = inventoryManager.getItemInSlot(station, slot);
        ItemComponent component = item.getComponent(ItemComponent.class);
        if (component == null)
            return false;

        BlockDamageComponent blockDamage = component.damageType.getComponent(BlockDamageComponent.class);

        return blockDamage != null && blockDamage.materialDamageMultiplier.containsKey(toolType)
                && item.hasComponent(DurabilityComponent.class) && item.getComponent(DurabilityComponent.class).durability >= durability;
    }

    @Override
    public CraftingStationResult getResultById(String resultId) {
        String[] split = resultId.split("\\|");
        List<Integer> result = new LinkedList<>();
        for (String s : split) {
            result.add(Integer.parseInt(s));
        }

        return new Result(result);
    }

    private class Result implements CraftingStationResult {
        private List<Integer> items;
        private List<Integer> tools;

        public Result(List<Integer> slots) {
            items = slots.subList(0, ingredientsMap.size());
            tools = slots.subList(ingredientsMap.size(), toolsMap.size());
        }

        @Override
        public String getResultId() {
            StringBuilder sb = new StringBuilder();
            for (Integer item : items) {
                sb.append(item).append("|");
            }
            for (Integer tool : tools) {
                sb.append(tool).append("|");
            }

            return sb.replace(sb.length() - 1, sb.length() + 1, "").toString();
        }

        @Override
        public EntityRef craftOne(EntityRef station, int componentFromSlot, int componentSlotCount, int toolFromSlot, int toolSlotCount) {
            if (!validateCreation(station)) return EntityRef.NULL;

            int index = 0;
            for (Map.Entry<String, Integer> ingredientCount : ingredientsMap.entrySet()) {
                inventoryManager.removeItem(station, inventoryManager.getItemInSlot(station, items.get(index)), ingredientCount.getValue());
                index++;
            }
            index = 0;
            for (Map.Entry<String, Integer> toolDurability : toolsMap.entrySet()) {
                final EntityRef tool = inventoryManager.getItemInSlot(station, tools.get(index));
                tool.send(new ReduceDurabilityEvent(toolDurability.getValue()));
                index++;
            }

            return createResultItemEntityForDisplayOne();
        }

        private boolean validateCreation(EntityRef station) {
            int index = 0;
            for (Map.Entry<String, Integer> ingredientCount : ingredientsMap.entrySet()) {
                if (!hasItemInSlot(station, ingredientCount.getKey(), items.get(index), ingredientCount.getValue()))
                    return false;
                index++;
            }
            index = 0;
            for (Map.Entry<String, Integer> toolDurability : toolsMap.entrySet()) {
                if (!hasToolInSlot(station, toolDurability.getKey(), tools.get(index), toolDurability.getValue()))
                    return false;
                index++;
            }
            return true;
        }

        @Override
        public EntityRef craftMax(EntityRef stationEntity, int componentFromSlot, int componentSlotCount, int toolFromSlot, int toolSlotCount, int resultSlot) {
            return null;
        }

        @Override
        public Map<Integer, Integer> getComponentSlotAndCount() {
            Map<Integer, Integer> result = new LinkedHashMap<>();
            int index = 0;
            for (Integer count : ingredientsMap.values()) {
                result.put(items.get(index), count);
                index++;
            }

            return result;
        }

        @Override
        public EntityRef createResultItemEntityForDisplayOne() {
            if (itemResult != null) {
                final EntityRef entity = CoreRegistry.get(EntityManager.class).create(itemResult);
                final ItemComponent item = entity.getComponent(ItemComponent.class);
                item.stackCount = count;
                entity.saveComponent(item);
                return entity;
            } else {
                return CoreRegistry.get(BlockItemFactory.class).newInstance(CoreRegistry.get(BlockManager.class).getBlockFamily(blockResult), count);
            }
        }

        @Override
        public EntityRef getResultItemEntityForDisplayMax() {
            return null;
        }
    }
}