package noobanidus.mods.lootr.data;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.NonNullList;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import noobanidus.mods.lootr.api.ILootTile;
import noobanidus.mods.lootr.api.ILootrInventory;
import noobanidus.mods.lootr.entity.LootrChestMinecartEntity;

import javax.annotation.Nullable;

@SuppressWarnings("NullableProblems")
public class SpecialChestInventory implements ILootrInventory {
  private NewChestData newChestData;
  private final NonNullList<ItemStack> contents;
  private final Component name;

  @Nullable
  private BlockPos pos;

  public SpecialChestInventory(NewChestData newChestData, NonNullList<ItemStack> contents, Component name, @Nullable BlockPos pos) {
    this.newChestData = newChestData;
    this.contents = contents;
    this.name = name;
    this.pos = pos;
  }

  public SpecialChestInventory(NewChestData newChestData, CompoundTag items, String componentAsJSON, BlockPos pos) {
    this.newChestData = newChestData;
    this.name = Component.Serializer.fromJson(componentAsJSON);
    this.contents = NonNullList.withSize(27, ItemStack.EMPTY);
    ContainerHelper.loadAllItems(items, this.contents);
    this.pos = pos;
  }

  public void setBlockPos(BlockPos pos) {
    this.pos = pos;
  }

  @Override
  @Nullable
  public RandomizableContainerBlockEntity getTile(Level world) {
    if (world == null || world.isClientSide() || pos == null) {
      return null;
    }

    BlockEntity te = world.getBlockEntity(pos);
    if (te instanceof ILootTile) {
      return (RandomizableContainerBlockEntity) te;
    }

    return null;
  }

  @Override
  @Nullable
  public LootrChestMinecartEntity getEntity(Level world) {
    if (world == null || world.isClientSide() || newChestData.getEntityId() == null) {
      return null;
    }

    if (!(world instanceof ServerLevel)) {
      return null;
    }

    ServerLevel serverWorld = (ServerLevel) world;
    Entity entity = serverWorld.getEntity(newChestData.getEntityId());
    if (entity instanceof LootrChestMinecartEntity) {
      return (LootrChestMinecartEntity) entity;
    }

    return null;
  }

  @Override
  public int getContainerSize() {
    return 27;
  }

  @Override
  public boolean isEmpty() {
    for (ItemStack itemstack : this.contents) {
      if (!itemstack.isEmpty()) {
        return false;
      }
    }

    return true;
  }

  @Override
  public ItemStack getItem(int index) {
    return contents.get(index);
  }

  @Override
  public ItemStack removeItem(int index, int count) {
    ItemStack itemstack = ContainerHelper.removeItem(this.contents, index, count);
    if (!itemstack.isEmpty()) {
      this.setChanged();
      // TODO: Trigger save?
    }

    return itemstack;
  }

  @Override
  public ItemStack removeItemNoUpdate(int index) {
    ItemStack result = ContainerHelper.takeItem(contents, index);
    if (!result.isEmpty()) {
      this.setChanged();
    }

    return result;
  }

  @Override
  public void setItem(int index, ItemStack stack) {
    this.contents.set(index, stack);
    if (stack.getCount() > this.getMaxStackSize()) {
      stack.setCount(this.getMaxStackSize());
    }

    this.setChanged();
  }

  @Override
  public void setChanged() {
    newChestData.setDirty();
  }

  @Override
  public boolean stillValid(Player player) {
    return true;
  }

  @Override
  public void clearContent() {
    contents.clear();
    setChanged();
  }

  @Override
  public Component getDisplayName() {
    return name;
  }

  @Nullable
  @Override
  public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
    return ChestMenu.threeRows(id, inventory, this);
  }

  @Override
  public void startOpen(Player player) {
    Level world = player.level;
    RandomizableContainerBlockEntity tile = getTile(world);
    if (tile != null) {
      tile.startOpen(player);
    }
    if (newChestData.getEntityId() != null) {
      LootrChestMinecartEntity entity = getEntity(world);
      if (entity != null) {
        entity.startOpen(player);
      }
    }
  }

  @Override
  public void stopOpen(Player player) {
    setChanged();
    Level world = player.level;
    if (pos != null) {
      RandomizableContainerBlockEntity tile = getTile(world);
      if (tile != null) {
        tile.stopOpen(player);
      }
    }
    if (newChestData.getEntityId() != null) {
      LootrChestMinecartEntity entity = getEntity(world);
      if (entity != null) {
        entity.stopOpen(player);
      }
    }
  }

  public CompoundTag writeItems() {
    CompoundTag result = new CompoundTag();
    return ContainerHelper.saveAllItems(result, this.contents);
  }

  public String writeName() {
    return Component.Serializer.toJson(this.name);
  }

  @Override
  @Nullable
  public BlockPos getPos() {
    return pos;
  }

  @Override
  public NonNullList<ItemStack> getContents() {
    return this.contents;
  }
}
