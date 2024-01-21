/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2022 retrooper and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.jtprince.coordinateoffset.offsetter.wrapper;

import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.mapper.MappedEntity;
import com.github.retrooper.packetevents.protocol.particle.data.ParticleData;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/*
 * CO: In PacketEvents 2.2.0, the wrapper does not properly wrap entity eye height. PR is opened here:
 * https://github.com/retrooper/packetevents/pull/661 Until this merges and a fixed PE release is made, this is a
 * workaround to use my own wrapper that does properly add this data.
 */
public class ParticleVibrationData_EyeHeight extends ParticleData {

    public enum PositionType implements MappedEntity {

        BLOCK(new ResourceLocation("minecraft:block")),
        ENTITY(new ResourceLocation("minecraft:entity"));

        private final ResourceLocation name;

        PositionType(ResourceLocation name) {
            this.name = name;
        }

        @Override
        public ResourceLocation getName() {
            return name;
        }

        public static PositionType getById(int id) {
            switch (id) {
                case 0x00:
                    return BLOCK;
                case 0x01:
                    return ENTITY;
                default:
                    throw new IllegalArgumentException("Illegal position type id: " + id);
            }
        }

        public static @Nullable PositionType getByName(String name) {
            return getByName(new ResourceLocation(name));
        }

        public static @Nullable PositionType getByName(ResourceLocation name) {
            for (PositionType type : values()) {
                if (type.getName().equals(name)) {
                    return type;
                }
            }
            return null;
        }

        @Override
        public int getId(ClientVersion version) {
            return this.ordinal();
        }
    }

    private Vector3i startingPosition; // Removed in 1.19.4
    private PositionType type;
    private @Nullable Vector3i blockPosition;
    private @Nullable Integer entityId;
    private @Nullable Float entityEyeHeight; // CO
    private int ticks;

    public ParticleVibrationData_EyeHeight(Vector3i startingPosition, @Nullable Vector3i blockPosition, int ticks) {
        this.startingPosition = startingPosition;
        this.type = PositionType.BLOCK;
        this.blockPosition = blockPosition;
        this.entityId = null;
        this.ticks = ticks;
    }

    public ParticleVibrationData_EyeHeight(Vector3i startingPosition, int entityId, float entityEyeHeight, int ticks) { // CO
        this.startingPosition = startingPosition;
        this.type = PositionType.ENTITY;
        this.blockPosition = null;
        this.entityId = entityId;
        this.entityEyeHeight = entityEyeHeight; // CO
        this.ticks = ticks;
    }

    public Vector3i getStartingPosition() {
        return startingPosition;
    }

    public void setStartingPosition(Vector3i startingPosition) {
        this.startingPosition = startingPosition;
    }

    public PositionType getType() {
        return type;
    }

    public Optional<Vector3i> getBlockPosition() {
        return Optional.ofNullable(blockPosition);
    }

    public void setBlockPosition(@Nullable Vector3i blockPosition) {
        this.blockPosition = blockPosition;
    }

    public Optional<Integer> getEntityId() {
        return Optional.ofNullable(entityId);
    }

    public void setEntityId(int entityId) {
        this.entityId = entityId;
    }

    // CO Start
    public Optional<Float> getEntityEyeHeight() {
        return Optional.ofNullable(entityEyeHeight);
    }

    public void setEntityEyeHeight(float entityEyeHeight) {
        this.entityEyeHeight = entityEyeHeight;
    }
    // CO End

    public int getTicks() {
        return ticks;
    }

    public void setTicks(int ticks) {
        this.ticks = ticks;
    }

    public static ParticleVibrationData_EyeHeight read(PacketWrapper<?> wrapper) {
        Vector3i startingPos = wrapper.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_19_4)
                ? Vector3i.zero() : wrapper.readBlockPosition();

        PositionType positionType;
        if (wrapper.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_20_3)) {
            positionType = PositionType.getById(wrapper.readVarInt());
        } else {
            String positionTypeName = wrapper.readString();
            positionType = PositionType.getByName(positionTypeName);
            if (positionType == null) {
                throw new IllegalArgumentException("Unknown position type: " + positionTypeName);
            }
        }

        switch (positionType) {
            case BLOCK:
                return new ParticleVibrationData_EyeHeight(startingPos, wrapper.readBlockPosition(), wrapper.readVarInt());
            case ENTITY:
                return new ParticleVibrationData_EyeHeight(startingPos, wrapper.readVarInt(), wrapper.readFloat(), wrapper.readVarInt()); // CO
            default:
                throw new IllegalArgumentException("Illegal position type: " + positionType);
        }
    }

    public static void write(PacketWrapper<?> wrapper, ParticleVibrationData_EyeHeight data) {
        if (wrapper.getServerVersion().isOlderThan(ServerVersion.V_1_19_4)) {
            wrapper.writeBlockPosition(data.getStartingPosition());
        }

        if (wrapper.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_20_3)) {
            wrapper.writeVarInt(data.getType().getId(wrapper.getServerVersion().toClientVersion()));
        } else {
            wrapper.writeIdentifier(data.getType().getName());
        }

        if (data.getType() == PositionType.BLOCK) {
            wrapper.writeBlockPosition(data.getBlockPosition().get());
        } else if (data.getType() == PositionType.ENTITY) {
            wrapper.writeVarInt(data.getEntityId().get());
            wrapper.writeFloat(data.getEntityEyeHeight().get()); // CO
        }
        wrapper.writeVarInt(data.getTicks());
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}
