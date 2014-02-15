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
package org.terasology.durability;

import org.terasology.asset.AssetUri;
import org.terasology.asset.Assets;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.math.Rect2i;
import org.terasology.math.Vector2i;
import org.terasology.rendering.assets.texture.Texture;
import org.terasology.rendering.assets.texture.TextureUtil;
import org.terasology.rendering.nui.Canvas;
import org.terasology.rendering.nui.layers.ingame.inventory.InventoryCellRendered;

import java.awt.*;

/**
 * @author Marcin Sciesinski <marcins78@gmail.com>
 */
@RegisterSystem(RegisterMode.CLIENT)
public class DurabilityClientSystem extends BaseComponentSystem {
    @ReceiveEvent(components = {DurabilityComponent.class})
    public void drawDurabilityBar(InventoryCellRendered event, EntityRef entity) {
        Canvas canvas = event.getCanvas();

        Vector2i size = canvas.size();

        int minX = (int) (size.x * 0.1f);
        int maxX = (int) (size.x * 0.9f);

        int minY = (int) (size.y * 0.8f);
        int maxY = (int) (size.y * 0.9f);

        DurabilityComponent durability = entity.getComponent(DurabilityComponent.class);
        float durabilityPercentage = 1f * durability.durability / durability.maxDurability;

        if (durabilityPercentage != 1f) {
            AssetUri backgroundTexture = TextureUtil.getTextureUriForColor(Color.WHITE);
            float red = Math.min(1, (1f - durabilityPercentage) * 2);
            float green = Math.min(1, durabilityPercentage * 2);

            AssetUri barTexture = TextureUtil.getTextureUriForColor(new Color(red, green, 0));

            canvas.drawTexture(Assets.get(backgroundTexture, Texture.class), Rect2i.createFromMinAndMax(minX, minY, maxX, maxY));
            int durabilityBarLength = (int) (durabilityPercentage * (maxX - minX - 1));
            int durabilityBarHeight = maxY - minY - 1;
            canvas.drawTexture(Assets.get(barTexture, Texture.class), Rect2i.createFromMinAndSize(minX + 1, minY + 1, durabilityBarLength, durabilityBarHeight));
        }
    }
}
