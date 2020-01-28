/*
 * This file is part of Nucleus, licensed under the MIT License (MIT). See the LICENSE.txt file
 * at the root of this project for more details.
 */
package io.github.nucleuspowered.nucleus.services.impl.storage.dataobjects.standard;

import com.google.common.collect.ImmutableMap;
import io.github.nucleuspowered.nucleus.api.nucleusdata.Kit;
import io.github.nucleuspowered.nucleus.services.impl.storage.dataobjects.configurate.IConfigurateBackedDataObject;

import java.util.Map;
import java.util.Optional;

public interface IKitDataObject extends IConfigurateBackedDataObject {

    ImmutableMap<String, Kit> getKitMap();

    void setKitMap(Map<String, Kit> map) throws Exception;

    boolean hasKit(String name);

    Optional<Kit> getKit(String name);

    void setKit(Kit kit) throws Exception;

    boolean removeKit(String name) throws Exception;

}
