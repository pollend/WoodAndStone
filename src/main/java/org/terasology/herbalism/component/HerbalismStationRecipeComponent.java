/*
 * Copyright 2016 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.herbalism.component;

import org.terasology.entitySystem.Component;
import java.util.List;

// Add this Component to any recipe prefab that is supposed to be creatable in a HerbalismStation or similar.
// Include in prefab along with CraftingStationRecipeComponent to work properly.
public class HerbalismStationRecipeComponent implements Component {
    // The following variables are unused.
    public String recipeId;
    public List<String> recipeComponents;
    public List<String> recipeTools;
    public List<String> recipeFluids;

    public float requiredTemperature;
    public long processingDuration;

    public String itemResult;
    public String blockResult;
}
