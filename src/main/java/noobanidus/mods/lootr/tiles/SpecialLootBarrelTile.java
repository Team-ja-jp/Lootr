package noobanidus.mods.lootr.tiles;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.util.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.LazyOptional;
import noobanidus.mods.lootr.api.ILootTile;
import noobanidus.mods.lootr.blocks.LootrBarrelBlock;
import noobanidus.mods.lootr.config.ConfigManager;
import noobanidus.mods.lootr.init.ModBlocks;
import noobanidus.mods.lootr.init.ModTiles;
import noobanidus.mods.lootr.util.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

@SuppressWarnings({"ConstantConditions", "NullableProblems", "WeakerAccess"})
public class SpecialLootBarrelTile extends BarrelBlockEntity implements ILootTile {
  public Set<UUID> openers = new HashSet<>();
  private int specialNumPlayersUsingBarrel;
  private ResourceLocation savedLootTable = null;
  private long seed = -1;
  private UUID tileId = null;

  public SpecialLootBarrelTile() {
    super(ModTiles.SPECIAL_LOOT_BARREL);
  }

  @Nonnull
  @Override
  public IModelData getModelData() {
    IModelData data = new ModelDataMap.Builder().withInitial(LootrBarrelBlock.OPENED, false).build();
    Player player = Getter.getPlayer();
    if (player != null) {
      data.setData(LootrBarrelBlock.OPENED, openers.contains(player.getUUID()));
    }
    return data;
  }

  @Override
  public UUID getTileId() {
    if (this.tileId == null) {
      this.tileId = UUID.randomUUID();
    }
    return this.tileId;
  }

  @Override
  public void setLootTable(ResourceLocation lootTableIn, long seedIn) {
    this.savedLootTable = lootTableIn;
    this.seed = seedIn;
    super.setLootTable(lootTableIn, seedIn);
  }

  @Override
  public void unpackLootTable(@Nullable Player player) {
    // TODO: Override
  }

  @Override
  @SuppressWarnings({"unused", "Duplicates"})
  public void fillWithLoot(Player player, Container inventory, @Nullable ResourceLocation overrideTable, long seed) {
    if (this.level != null && this.savedLootTable != null && this.level.getServer() != null) {
      LootTable loottable = this.level.getServer().getLootTables().get(overrideTable != null ? overrideTable : this.savedLootTable);
      if (player instanceof ServerPlayer) {
        CriteriaTriggers.GENERATE_LOOT.trigger((ServerPlayer) player, overrideTable != null ? overrideTable : this.lootTable);
      }
      LootContext.Builder builder = (new LootContext.Builder((ServerLevel) this.level)).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(this.worldPosition)).withOptionalRandomSeed(ConfigManager.RANDOMISE_SEED.get() ? ThreadLocalRandom.current().nextLong() : seed == Long.MIN_VALUE ? this.seed : seed);
      if (player != null) {
        builder.withLuck(player.getLuck()).withParameter(LootContextParams.THIS_ENTITY, player);
      }

      loottable.fill(inventory, builder.create(LootContextParamSets.CHEST));
    }
  }

  @Override
  public ResourceLocation getTable() {
    return savedLootTable;
  }

  @Override
  public long getSeed() {
    return seed;
  }

  @Override
  public Set<UUID> getOpeners() {
    return openers;
  }

  @SuppressWarnings("Duplicates")
  @Override
  public void load(BlockState state, CompoundTag compound) {
    if (compound.contains("specialLootChest_table", Constants.NBT.TAG_STRING)) {
      savedLootTable = new ResourceLocation(compound.getString("specialLootChest_table"));
    }
    if (compound.contains("specialLootChest_seed", Constants.NBT.TAG_LONG)) {
      seed = compound.getLong("specialLootChest_seed");
    }
    if (savedLootTable == null && compound.contains("LootTable", Constants.NBT.TAG_STRING)) {
      savedLootTable = new ResourceLocation(compound.getString("LootTable"));
      if (compound.contains("LootTableSeed", Constants.NBT.TAG_LONG)) {
        seed = compound.getLong("LootTableSeed");
      }
      setLootTable(savedLootTable, seed);
    }
    if (compound.hasUUID("tileId")) {
      this.tileId = compound.getUUID("tileId");
    } else if (this.tileId == null) {
      getTileId();
    }
    if (compound.contains("LootrOpeners")) {
      ListTag openers = compound.getList("LootrOpeners", Constants.NBT.TAG_INT_ARRAY);
      this.openers.clear();
      for (Tag item : openers) {
        this.openers.add(NbtUtils.loadUUID(item));
      }
    }
    requestModelDataUpdate();
    super.load(state, compound);
  }

  @Override
  public CompoundTag save(CompoundTag compound) {
    compound = super.save(compound);
    if (savedLootTable != null) {
      compound.putString("specialLootBarrel_table", savedLootTable.toString());
      compound.putString("LootTable", savedLootTable.toString());
    }
    if (seed != -1) {
      compound.putLong("specialLootBarrel_seed", seed);
      compound.putLong("LootTableSeed", seed);
    }
    compound.putUUID("tileId", getTileId());
    ListTag list = new ListTag();
    for (UUID opener : this.openers) {
      list.add(NbtUtils.createUUID(opener));
    }
    compound.put("LootrOpeners", list);
    return compound;
  }

  @Override
  public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
    return LazyOptional.empty();
  }

  @Override
  public void recheckOpen() {
    int x = this.worldPosition.getX();
    int y = this.worldPosition.getY();
    int z = this.worldPosition.getZ();
    this.specialNumPlayersUsingBarrel = SpecialLootChestTile.calculatePlayersUsing(this.level, this, x, y, z);
    if (this.specialNumPlayersUsingBarrel > 0) {
      this.scheduleTick();
    } else {
      BlockState state = this.getBlockState();
      if (state.getBlock() != ModBlocks.BARREL && state.getBlock() != Blocks.BARREL) {
        this.setRemoved();
        return;
      }

      boolean open = state.getValue(BarrelBlock.OPEN);
      if (open) {
        this.playSound(state, SoundEvents.BARREL_CLOSE);
        this.setOpenProperty(state, false);
      }
    }
  }

  private void setOpenProperty(BlockState state, boolean open) {
    this.level.setBlock(this.getBlockPos(), state.setValue(BarrelBlock.OPEN, open), 3);
  }

  private void playSound(BlockState state, SoundEvent sound) {
    Vec3i dir = state.getValue(BarrelBlock.FACING).getNormal();
    double x = (double) this.worldPosition.getX() + 0.5D + (double) dir.getX() / 2.0D;
    double y = (double) this.worldPosition.getY() + 0.5D + (double) dir.getY() / 2.0D;
    double z = (double) this.worldPosition.getZ() + 0.5D + (double) dir.getZ() / 2.0D;
    this.level.playSound(null, x, y, z, sound, SoundSource.BLOCKS, 0.5F, this.level.random.nextFloat() * 0.1F + 0.9F);
  }

  private void scheduleTick() {
    this.level.getBlockTicks().scheduleTick(this.getBlockPos(), this.getBlockState().getBlock(), 5);
  }

  @Override
  public void startOpen(Player player) {
    if (!player.isSpectator()) {
      if (this.specialNumPlayersUsingBarrel < 0) {
        this.specialNumPlayersUsingBarrel = 0;
      }

      ++this.specialNumPlayersUsingBarrel;
      BlockState state = this.getBlockState();
      boolean open = state.getValue(BarrelBlock.OPEN);
      if (!open) {
        this.playSound(state, SoundEvents.BARREL_OPEN);
        this.setOpenProperty(state, true);
      }

      this.scheduleTick();
    }
  }

  @Override
  public void stopOpen(Player player) {
    if (!player.isSpectator()) {
      --this.specialNumPlayersUsingBarrel;
      openers.add(player.getUUID());
      this.setChanged();
      updatePacketViaState();
    }
  }

  @Override
  public void updatePacketViaState() {
    if (level != null && !level.isClientSide) {
      BlockState state = level.getBlockState(getBlockPos());
      level.sendBlockUpdated(getBlockPos(), state, state, 8);
    }
  }


  @Override
  @Nonnull
  public CompoundTag getUpdateTag() {
    return save(new CompoundTag());
  }

  @Override
  @Nullable
  public ClientboundBlockEntityDataPacket getUpdatePacket() {
    return new ClientboundBlockEntityDataPacket(getBlockPos(), 0, getUpdateTag());
  }

  @Override
  public void onDataPacket(@Nonnull Connection net, @Nonnull ClientboundBlockEntityDataPacket pkt) {
    load(ModBlocks.CHEST.defaultBlockState(), pkt.getTag());
  }
}
