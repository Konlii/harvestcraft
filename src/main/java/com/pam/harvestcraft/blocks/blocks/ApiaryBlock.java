package com.pam.harvestcraft.blocks.blocks;

import java.util.List;
import java.util.Random;

import com.pam.harvestcraft.HarvestCraft;
import com.pam.harvestcraft.ItemStackUtils;
import com.pam.harvestcraft.blocks.BlockRegistry;
import com.pam.harvestcraft.gui.GuiHandler;
import com.pam.harvestcraft.tileentities.TileEntityApiary;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public class ApiaryBlock extends BlockContainerRotating {

	public static final String registryName = "apiary";
	private static boolean keepInventory;

	public ApiaryBlock() {
		super(Material.WOOD);
		setSoundType(SoundType.WOOD);
		setCreativeTab(HarvestCraft.modTab);
	}

	@Override
	public TileEntity createNewTileEntity(World worldIn, int meta) {
		return new TileEntityApiary();
	}
	
	@Override
    public void onBlockHarvested(World worldIn, BlockPos pos, IBlockState state, EntityPlayer player)
    {
    	TileEntityApiary tileEntityApiary = (TileEntityApiary)worldIn.getTileEntity(pos);
    	tileEntityApiary.setDestroyedByCreativePlayer(player.capabilities.isCreativeMode);
    }

    @Override
	public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
		TileEntity tileentity = worldIn.getTileEntity(pos);
		keepInventory = true;
				
		if(tileentity instanceof TileEntityApiary) {
			TileEntityApiary tileEntityApiary = (TileEntityApiary)tileentity;
			
			if (!tileEntityApiary.isDestroyedByCreativePlayer())
			{
				if (!keepInventory)
				{
					spawnAsEntity(worldIn, pos, new ItemStack(BlockRegistry.apiaryItemBlock));
					ItemStackUtils.dropInventoryItems(worldIn, pos,
							tileEntityApiary.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null));
				}
				else if (tileEntityApiary.shouldDrop())
                {
					ItemStack itemstack = new ItemStack(Item.getItemFromBlock(this));
					NBTTagCompound nbttagcompound = new NBTTagCompound();
					NBTTagCompound nbttagcompound1 = new NBTTagCompound();
					nbttagcompound.setTag("BlockEntityTag", tileEntityApiary.saveToNbt(nbttagcompound1));
					itemstack.setTagCompound(nbttagcompound);
                	spawnAsEntity(worldIn, pos, itemstack);
                }
                else
                {
                	spawnAsEntity(worldIn, pos, new ItemStack(BlockRegistry.apiaryItemBlock));
                }
			}
		
			worldIn.updateComparatorOutputLevel(pos, this);
		}
		
		super.breakBlock(worldIn, pos, state);
	}
	
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced)
    {
       super.addInformation(stack, player, tooltip, advanced);
       NBTTagCompound nbttagcompound = stack.getTagCompound();

        if (nbttagcompound != null && nbttagcompound.hasKey("BlockEntityTag", 10))
        {
        	NBTTagCompound nbttagcompound1 = nbttagcompound.getCompoundTag("BlockEntityTag");

        	ItemStackHandler itemstackhandler = new ItemStackHandler(19);
        	itemstackhandler.deserializeNBT((NBTTagCompound) nbttagcompound1.getTag("Items"));
        	int i = 0;
        	int j = 0;

        	for (int slot=18; slot>=0; slot--)
        	{
        		ItemStack itemstack = itemstackhandler.getStackInSlot(slot);
        		if (!itemstack.isEmpty())
        		{
        			++j;

        			if (i <= 4)
        			{
        				++i;
        				tooltip.add(String.format("%s x%d", new Object[] {itemstack.getDisplayName(), Integer.valueOf(itemstack.getCount())}));
        			}
        		}
        	}

        	if (j - i > 0)
        	{
        		tooltip.add(TextFormatting.ITALIC + I18n.format("container.shulkerBox.more", new Object[] {Integer.valueOf(j - i)}));
        	}
        }
    }

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
			EnumFacing side, float hitX, float hitY, float hitZ) {
		if(world.isRemote) {
			return true;
		}
		TileEntity te = world.getTileEntity(pos);
		if(!(te instanceof TileEntityApiary)) {
			return false;
		}
		player.openGui(HarvestCraft.instance, GuiHandler.GUIID_APIARY, world, pos.getX(), pos.getY(), pos.getZ());
		return true;
	}

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
    	return null;
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player)
    {	
        ItemStack itemstack = super.getPickBlock(state, target, world, pos, player);
        TileEntityApiary tileEntityApiary = (TileEntityApiary)world.getTileEntity(pos);
        NBTTagCompound nbttagcompound = tileEntityApiary.saveToNbt(new NBTTagCompound());

        if (!nbttagcompound.hasNoTags())
        {
            itemstack.setTagInfo("BlockEntityTag", nbttagcompound);
        }

        return itemstack;
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
    	super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
    }
    
    @Override
    public boolean canPlaceBlockAt(World worldIn, BlockPos pos)
    {
        return worldIn.getBlockState(pos).getBlock().isReplaceable(worldIn, pos) && worldIn.getBlockState(pos.down()).isSideSolid(worldIn, pos, EnumFacing.UP);
    }

    
    @Override
    public EnumPushReaction getMobilityFlag(IBlockState state)
    {
        return EnumPushReaction.DESTROY;
    }
}