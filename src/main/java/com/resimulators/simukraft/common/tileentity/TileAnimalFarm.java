package com.resimulators.simukraft.common.tileentity;

import com.resimulators.simukraft.client.gui.GuiHandler;
import com.resimulators.simukraft.common.enums.Animal;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.server.ServerWorld;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class TileAnimalFarm extends TileEntity implements IControlBlock{

    private boolean hired;
    private UUID simId;
    int maxAnimals = 0;
    Animal entity;

    public TileAnimalFarm(TileEntityType<?> tileEntityTypeIn, Animal entity) {
        super(tileEntityTypeIn);
        this.entity = entity;
        maxAnimals = 5;
    }

    @Override
    public void setHired(boolean hired) {
        this.hired = hired;
    }

    @Override
    public boolean getHired() {
        return hired;
    }

    @Override
    public UUID getSimId() {
        return simId;
    }

    @Override
    public void setSimId(UUID id) {
        this.simId = id;
    }

    @Override
    public int getGui() {
        return GuiHandler.ANIMAL_FARM;
    }


    public void spawnAnimal(){
        if (world != null){
            entity.getAnimal().spawn((ServerWorld) world,null,null,pos.add(0,1,0), SpawnReason.TRIGGERED,false , false);
        }

    }

    public int checkForAnimals(){
        if (world != null){
        AxisAlignedBB area = new AxisAlignedBB(this.pos.add(-4,0,-4),this.pos.add(4,1,4));
        List<AnimalEntity> entities = world.getLoadedEntitiesWithinAABB(AnimalEntity.class,area);
        entities = entities.stream().filter(animal -> animal.getType() == entity.getAnimal())
        .collect(Collectors.toList());
        return entities.size();
        }
        return maxAnimals; // basically force the tile entity not to spawn anything new
    }

    public int getMaxAnimals(){
       return maxAnimals;
    }

    public boolean hasMaxAnimals(){
        return maxAnimals == checkForAnimals();
    }


}