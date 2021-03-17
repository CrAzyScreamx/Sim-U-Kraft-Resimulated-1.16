package com.resimulators.simukraft.common.entity.goals;

import com.resimulators.simukraft.SimuKraft;
import com.resimulators.simukraft.common.building.BuildingTemplate;
import com.resimulators.simukraft.common.entity.sim.SimEntity;
import com.resimulators.simukraft.common.enums.BuildingType;
import com.resimulators.simukraft.common.jobs.JobBuilder;
import com.resimulators.simukraft.common.jobs.core.Activity;
import com.resimulators.simukraft.common.tileentity.TileResidential;
import com.resimulators.simukraft.common.world.Faction;
import com.resimulators.simukraft.common.world.SavedWorldData;
import com.resimulators.simukraft.handlers.StructureHandler;
import com.resimulators.simukraft.init.ModBlockProperties;
import com.resimulators.simukraft.init.ModBlocks;
import com.resimulators.simukraft.utils.BlockUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.Path;
import net.minecraft.tileentity.ChestTileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.template.PlacementSettings;
import net.minecraft.world.gen.feature.template.Template;

import java.util.*;
import java.util.stream.Collectors;

public class BuilderGoal extends BaseGoal<JobBuilder> {
    private final SimEntity sim;
    private int tick;
    private BuildingTemplate template;
    private List<Template.BlockInfo> blocks;
    private State state = State.STARTING;
    private int blockIndex = 0;
    private int delay = 20;
    private int notifyDelay = 0;
    Rotation rotation;
    private ArrayList<BlockPos> chests = new ArrayList<>();
    private HashMap<Item,Integer> blocksNeeded = new HashMap<>();
    public BuilderGoal(SimEntity sim) {
        super(sim, .7d, 20);
        this.sim = sim;
    }

    @Override
    public boolean canUse() {
        job = ((JobBuilder) sim.getJob());
        //System.out.println("startExecuting");
        if (job != null) {
            template = job.getTemplate();
            if (sim.getActivity() == Activity.GOING_TO_WORK) {
                checkForInventories();
                if (chests.size() != 0){
                    if (sim.blockPosition().closerThan(new Vector3d(job.getWorkSpace().getX(), job.getWorkSpace().getY(), job.getWorkSpace().getZ()), 5)) {
                        sim.setActivity(Activity.WORKING);
                        return template != null;
                    }
                }
                else {
                    if (notifyDelay <= 0) {
                        SavedWorldData.get(sim.level).getFactionWithSim(sim.getUUID()).sendFactionChatMessage(sim.getName().getString() + " Builder needs a chest to start Building",sim.level);
                        notifyDelay = 2000;
                    }else{
                        notifyDelay--;
                    }
                }
            }
        }
        return false;
    }


    @Override
    public void start() {
        template = job.getTemplate();

        if (template != null) {

            Rotation orgDir = getRotation(template.getDirection());
            Rotation facing = getRotation(job.getDirection());
            rotation = getRotationCalculated(orgDir,facing);

            PlacementSettings settings = new PlacementSettings()
                .setRotation(rotation)
                .setMirror(Mirror.NONE);
            blocks = StructureHandler.modifyAndConvertTemplate(template, sim.level, sim.getJob().getWorkSpace().relative(job.getDirection()),settings);
            SimuKraft.LOGGER().debug("cost: " + template.getCost());
            setBlocksNeeded();
            template.placeInWorld((IServerWorld) sim.level,sim.getJob().getWorkSpace().relative(job.getDirection()),settings,new Random());
        }
    }

    @Override
    public void tick() {
        tick++;
        super.tick();
        if (delay >= 0){
            delay = 60;
            if (state == State.STARTING){
                /*
                 setBlocksNeeded();
                 retrieveItemsFromChest();
                 un-comment for official release
                */
                state = State.TRAVELING;
                blockPos = blocks.get(blockIndex).pos;
//                while (sim.getCommandSenderWorld().getBlockState(blockPos) ==blocks.get(blockIndex).state){
//                    blockIndex++;
//                    if (blockIndex < blocks.size()) {
//                        blockPos = blocks.get(blockIndex).pos;
//                    }
//                }
            }
            if (state == State.TRAVELING){
                if(sim.getCommandSenderWorld().getBlockState(blockPos) ==blocks.get(blockIndex).state){
                    blockPos = blocks.get(blockIndex).pos;
                }
                if (sim.distanceToSqr(blockPos.getX(),blockPos.getY(),blockPos.getZ()) < 20){
                    state = State.BUILDING;
                    return;
                }
            }
            if (state == State.BUILDING){
                if (blockIndex < blocks.size()){
                    Template.BlockInfo blockInfo = blocks.get(blockIndex);
                    BlockState blockstate = blockInfo.state;
                    if (blockInfo.state.getBlock() == ModBlocks.CONTROL_BLOCK.get()) {
                        blockstate = blockInfo.state.with(ModBlockProperties.TYPE, template.getTypeID());
                        template.setControlBlock(blockInfo.pos);
                    }
                    if (sim.getInventory().hasItemStack(new ItemStack(blockInfo.state.getBlock())) || true){ // remove true for official release. for testing purposes
                        //BlockState blockState = sim.world.getBlockState(blockInfo.pos);
                        sim.world.destroyBlock(blockInfo.pos,true);
                        sim.world.setBlockState(blockInfo.pos, blockstate.rotate(sim.world,blockInfo.pos,rotation), 3);
                        int index = sim.getInventory().findSlotMatchingUnusedItem(new ItemStack(blockInfo.state.getBlock()));
                        if (index >= 0){
                        sim.getInventory().decrStackSize(index,1);}
                        blockIndex++;
                        if (blockIndex < blocks.size() - 1) {
                            destinationBlock = blocks.get(blockIndex).pos;
                            sim.getNavigator().setPath(null, 7d);
                            state = State.TRAVELING;
                        }
                    }else{
                        state = State.COLLECTING;
                        destinationBlock = job.getWorkSpace();
                    }
                }
            }

            if (state == State.COLLECTING){
                if (sim.distanceToSqr(blockPos.getX(),blockPos.getY(),blockPos.getZ()) < 10){
                    retrieveItemsFromChest();
                    state = State.STARTING;
                }

            }
        } else {
            delay--;
        }
        if (blockIndex >= blocks.size()){
            sim.getJob().setState(Activity.FORCE_STOP);

            Faction faction = SavedWorldData.get(sim.getCommandSenderWorld()).getFactionWithSim(sim.getUUID());
            faction.sendFactionChatMessage("Builder " + sim.getName().getString() + "has finished building " + template.getName(), sim.getCommandSenderWorld());
            if (template.getTypeID() == BuildingType.RESIDENTIAL.id) {
                BlockPos controlBlock = template.getControlBlock();
                UUID id = faction.addNewHouse(controlBlock, template.getName(), template.getRent());
                TileResidential tile =(TileResidential) sim.getCommandSenderWorld().getBlockEntity(controlBlock);
                tile.setFactionID(faction.getId());
                tile.setHouseID(id);

                blockIndex = 0;

                template = null;
                sim.fireSim(sim, faction.getId(), false);
            }
        }
    }

    @Override
    public boolean shouldRecalculatePath() {
        return sim.distanceToSqr(blockPos.getX(),blockPos.getY(),blockPos.getZ()) > acceptedDistance();
    }

    @Override
    protected boolean isValidTarget(IWorldReader iWorldReader, BlockPos blockPos) {
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (sim.getJob() != null){
        if (sim.getJob().getState() == Activity.FORCE_STOP) {
            return false;
        }
        if (tick < sim.getJob().workTime()) {
            return true;
        } else {
            sim.getJob().finishedWorkPeriod();
            sim.getJob().setState(Activity.NOT_WORKING);
        }}

        return false;
    }

    private Rotation getRotation(Direction dir){
        switch (dir){
            case NORTH:
                return Rotation.CLOCKWISE_90;
            case SOUTH:
                return Rotation.COUNTERCLOCKWISE_90;
            case WEST:
                return Rotation.CLOCKWISE_180;
            case EAST:
                return Rotation.NONE;

        }
        return Rotation.NONE;

    }

    private Rotation getRotationCalculated(Rotation org, Rotation cur) {
        if (org == cur.add(Rotation.CLOCKWISE_90)){
            return org.add(Rotation.COUNTERCLOCKWISE_90);
        }
        if (org == cur.add(Rotation.COUNTERCLOCKWISE_90)){
            return org.add(Rotation.CLOCKWISE_90);
        }
        if (org == cur.add(Rotation.CLOCKWISE_180)){
            return org.add(Rotation.CLOCKWISE_180);
        }

        return org;
    }


    private Direction rotateDirection(Direction dir, Rotation rotation){
        switch (rotation){
            case NONE:
                return dir;
            case CLOCKWISE_90:
                return dir.getClockWise();
            case CLOCKWISE_180:
                return dir.getOpposite();
            case COUNTERCLOCKWISE_90:
                return dir.getCounterClockWise();

        }

        return Direction.SOUTH;
    }
    //Checks for inventories around position.
    private void checkForInventories() {
        ArrayList<BlockPos> blocks =  BlockUtils.getBlocksAroundAndBelowPosition(job.getWorkSpace(),5);
        blocks.addAll(BlockUtils.getBlocksAroundAndBelowPosition(job.getWorkSpace().above(),5));
        blocks = (ArrayList<BlockPos>) blocks.stream().filter(pos -> sim.level.getBlockEntity(pos) != null).collect(Collectors.toList());
        for (BlockPos pos: blocks){
            if (sim.level.getBlockEntity(pos) instanceof ChestTileEntity){
                chests.add(pos);
            }
        }
    }

    private void setBlocksNeeded(){
        blocksNeeded.clear();
        for (int i = this.blockIndex; i< blocks.size();i++){
            Template.BlockInfo info = blocks.get(i);
            Block block = info.state.getBlock();
            if (block == Blocks.AIR) continue;
            if (blocksNeeded.get(block.asItem()) == null){
                blocksNeeded.put(block.asItem(),1);
            }else{
                int value = blocksNeeded.get(block.asItem());
                value += 1;
                blocksNeeded.put(block.asItem(),value);

            }

        }

    }
    //Scans inventories and makes Sim go get items from chest that contains it.
    private void retrieveItemsFromChest() {
        for (BlockPos pos: chests){
            ChestTileEntity entity = (ChestTileEntity) sim.level.getBlockEntity(pos);
            for (int i = 0; i< entity.getContainerSize();i++){
                ItemStack stack = entity.getItem(i);
                if (blocksNeeded.containsKey(stack.getItem())){
                    int amountNeeded = blocksNeeded.get(stack.getItem());

                    if (amountNeeded < stack.getCount()){
                        stack.setCount(stack.getCount()-amountNeeded);
                        blocksNeeded.remove(stack.getItem());
                    }else{
                        amountNeeded -= stack.getCount();
                        blocksNeeded.put(stack.getItem(),amountNeeded);
                        stack.setCount(0);
                    }
                    ItemStack result = stack.copy();
                    result.setCount(amountNeeded);
                    sim.getInventory().addItemStackToInventory(result);
                    entity.setItem(i,stack);

                }
            }

        }
        final String[] string = {""};
            blocksNeeded.keySet().forEach(key -> {
            int amount = blocksNeeded.get(key);
             string[0] += amount + " " + key + " ";

        });
            if (!string[0].equals("")){
        SavedWorldData.get(sim.level).getFactionWithSim(sim.getUUID()).sendFactionChatMessage(sim.getName().getString() + " still needs " + string[0],sim.level);
        delay = 2000;
    }}

    private enum State {
        STARTING,
        TRAVELING,
        BUILDING,
        WAITING,
        COLLECTING
    }
}
