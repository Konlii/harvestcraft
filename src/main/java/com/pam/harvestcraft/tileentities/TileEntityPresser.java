package com.pam.harvestcraft.tileentities;

import javax.annotation.Nullable;

import com.pam.harvestcraft.item.PresserRecipes;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public class TileEntityPresser extends TileEntity implements ITickable {

	private ItemStackHandler itemstackhandler = new ItemStackHandler(3);
	public short cookTime;

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if(capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
			return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(itemstackhandler);
		}
		return super.getCapability(capability, facing);
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		itemstackhandler.deserializeNBT((NBTTagCompound) compound.getTag("Items"));
		cookTime = compound.getShort("CookTime");
		super.readFromNBT(compound);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setShort("CookTime", cookTime);
		compound.setTag("Items", itemstackhandler.serializeNBT());

		return super.writeToNBT(compound);
	}

	@SideOnly(value = Side.CLIENT)
	public int getCookProgressScaled(int scale) {
		return cookTime * scale / 125;
	}

	@Override
	public void update() {
		boolean needsUpdate = false;

		if(world.isRemote)
			return;

		if(canRun()) {
			++cookTime;

			if(cookTime >= 125) {
				cookTime = 0;
				pressComb();
				needsUpdate = true;
			}
		}
		else {
			cookTime = 0;
		}

		if(needsUpdate != cookTime > 0) {
			needsUpdate = true;
		}

		if(needsUpdate) {
			markDirty();
			world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
		}
	}

	private boolean canRun() {
		if(itemstackhandler.getStackInSlot(0).isEmpty())
			return false;

		final ItemStack[] results = PresserRecipes.getPressingResult(itemstackhandler.getStackInSlot(0));
		if(results == null)
			return false;

		if(!itemstackhandler.getStackInSlot(1).isEmpty()) {
			if(!itemstackhandler.getStackInSlot(1).isItemEqual(results[0]))
				return false;
			if(itemstackhandler.getStackInSlot(1).getCount() + results[0].getCount() > itemstackhandler
					.getStackInSlot(1).getMaxStackSize())
				return false;
		}

		if(results[1] != null && !results[1].isEmpty() && !itemstackhandler.getStackInSlot(2).isEmpty()) {
			if(!itemstackhandler.getStackInSlot(2).isItemEqual(results[1]))
				return false;
			if(itemstackhandler.getStackInSlot(2).getCount() + results[1].getCount() > itemstackhandler
					.getStackInSlot(2).getMaxStackSize())
				return false;
		}

		return true;
	}

	private void pressComb() {

		if(!canRun())
			return;

		final ItemStack[] results = PresserRecipes.getPressingResult(itemstackhandler.getStackInSlot(0));
		if(results == null)
			return;

		if(itemstackhandler.getStackInSlot(1).isEmpty()) {

			itemstackhandler.setStackInSlot(1, results[0].copy());
		}
		else if(itemstackhandler.getStackInSlot(1).getCount() + results[0].getCount() <= results[0].getMaxStackSize()) {
			itemstackhandler.getStackInSlot(1)
					.setCount(itemstackhandler.getStackInSlot(1).getCount() + results[0].getCount());
		}

		if(!results[1].isEmpty()) {
			if(itemstackhandler.getStackInSlot(2).isEmpty()) {
				itemstackhandler.setStackInSlot(2, results[1].copy());
			}
			else if(itemstackhandler.getStackInSlot(2).isItemEqual(results[1])) {
				itemstackhandler.getStackInSlot(2)
						.setCount(itemstackhandler.getStackInSlot(2).getCount() + results[1].getCount());
			}
		}

		itemstackhandler.getStackInSlot(0).setCount(itemstackhandler.getStackInSlot(0).getCount() - 1);
		if(itemstackhandler.getStackInSlot(0).getCount() <= 0) {
			itemstackhandler.getStackInSlot(0).isEmpty();
		}
	}

	@Nullable
	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		final NBTTagCompound tag = new NBTTagCompound();
		writeToNBT(tag);

		return new SPacketUpdateTileEntity(getPos(), 1, tag);
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		readFromNBT(pkt.getNbtCompound());
	}

	public boolean canInteractWith(EntityPlayer playerIn) {
		// If we are too far away from this tile entity you cannot use it
		return !isInvalid() && playerIn.getDistanceSq(pos.add(0.5D, 0.5D, 0.5D)) <= 64D;
	}

	public String getGuiID() {
		return "harvestcraft:presser";
	}
}
