/**
 * This class was created by <Vazkii>. It's distributed as
 * part of the Botania Mod. Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 *
 * File Created @ [Jan 21, 2014, 7:51:36 PM (GMT)]
 */
package vazkii.botania.common.block.tile;

import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.registries.ObjectHolder;
import vazkii.botania.api.BotaniaAPI;
import vazkii.botania.api.internal.VanillaPacketDispatcher;
import vazkii.botania.api.item.IPetalApothecary;
import vazkii.botania.api.recipe.ICustomApothecaryColor;
import vazkii.botania.api.recipe.RecipePetals;
import vazkii.botania.client.core.handler.HUDHandler;
import vazkii.botania.client.core.helper.RenderHelper;
import vazkii.botania.common.Botania;
import vazkii.botania.common.block.ModBlocks;
import vazkii.botania.common.core.handler.ModSounds;
import vazkii.botania.common.lib.LibBlockNames;
import vazkii.botania.common.lib.LibMisc;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class TileAltar extends TileSimpleInventory implements IPetalApothecary, ITickableTileEntity {

	@ObjectHolder(LibMisc.MOD_ID + ":" + LibBlockNames.ALTAR)
	public static TileEntityType<TileAltar> TYPE;
	private static final Pattern SEED_PATTERN = Pattern.compile("(?:(?:(?:[A-Z-_.:]|^)seed)|(?:(?:[a-z-_.:]|^)Seed))(?:[sA-Z-_.:]|$)");
	private static final int SET_KEEP_TICKS_EVENT = 0;
	private static final int CRAFT_EFFECT_EVENT = 1;

	private static final String TAG_HAS_WATER = "hasWater";
	private static final String TAG_HAS_LAVA = "hasLava";

	private static final String ITEM_TAG_APOTHECARY_SPAWNED = "ApothecarySpawned";

	public boolean hasWater = false;
	public boolean hasLava = false;

	private List<ItemStack> lastRecipe = null;
	private int recipeKeepTicks = 0;

	public TileAltar() {
		super(TYPE);
	}

	public boolean collideEntityItem(ItemEntity item) {
		ItemStack stack = item.getItem();
		if(world.isRemote || stack.isEmpty() || !item.isAlive())
			return false;

		if(getBlockState().getBlock() == ModBlocks.defaultAltar && stack.getItem() == Item.getItemFromBlock(Blocks.VINE)) {
			ItemStack[] tmp = new ItemStack[getSizeInventory()];
			for(int i = 0; i < getSizeInventory(); i++) {
				tmp[i] = itemHandler.getStackInSlot(i);
				itemHandler.setStackInSlot(i, ItemStack.EMPTY);
			}

			stack.shrink(1);
			world.setBlockState(getPos(), ModBlocks.mossyAltar.getDefaultState());

			TileEntity newAltar = world.getTileEntity(getPos());
			if(newAltar instanceof TileAltar) {
				for(int i = 0; i < getSizeInventory(); i++) {
					((TileAltar) newAltar).getItemHandler().setStackInSlot(i, tmp[i]);
				}
			}

			return true;
		}

		boolean hasFluidCapability = stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY).isPresent();
		
		if(!hasWater() && !hasLava()) {
			if(hasFluidCapability) {
				/* todo 1.13
				IFluidHandlerItem fluidHandler = stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);

				FluidStack drainWater = fluidHandler.drain(new FluidStack(FluidRegistry.WATER, Fluid.BUCKET_VOLUME), false);
				FluidStack drainLava = fluidHandler.drain(new FluidStack(FluidRegistry.LAVA, Fluid.BUCKET_VOLUME), false);

				if(drainWater != null && drainWater.getFluid() == FluidRegistry.WATER && drainWater.amount == Fluid.BUCKET_VOLUME) {
					setWater(true);
					world.updateComparatorOutputLevel(pos, world.getBlockState(pos).getBlock());
					fluidHandler.drain(new FluidStack(FluidRegistry.WATER, Fluid.BUCKET_VOLUME), true);
					item.setItem(fluidHandler.getContainer());
					return true;
				} else if(drainLava != null && drainLava.getFluid() == FluidRegistry.LAVA && drainLava.amount == Fluid.BUCKET_VOLUME) {
					setLava(true);
					world.updateComparatorOutputLevel(pos, world.getBlockState(pos).getBlock());
					fluidHandler.drain(new FluidStack(FluidRegistry.LAVA, Fluid.BUCKET_VOLUME), true);
					item.setItem(fluidHandler.getContainer());
					return true;
				}
				*/
			}

			return false;
		}

		if(hasLava()) {
			item.setFire(100);
			return true;
		}
		
		if(SEED_PATTERN.matcher(stack.getTranslationKey()).find()) {
			for(RecipePetals recipe : BotaniaAPI.petalRecipes) {
				if(recipe.matches(itemHandler)) {
					saveLastRecipe();
					
					for(int i = 0; i < getSizeInventory(); i++)
						itemHandler.setStackInSlot(i, ItemStack.EMPTY);
					
					stack.shrink(1);
					
					ItemStack output = recipe.getOutput().copy();
					ItemEntity outputItem = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, output);
					outputItem.addTag(ITEM_TAG_APOTHECARY_SPAWNED);
					world.addEntity(outputItem);
					
					setWater(false);
					world.updateComparatorOutputLevel(pos, world.getBlockState(pos).getBlock());
					
					world.addBlockEvent(getPos(), getBlockState().getBlock(), CRAFT_EFFECT_EVENT, 0);
					
					return true;
				}
			}
		} else if(!hasFluidCapability && !item.getTags().contains(ITEM_TAG_APOTHECARY_SPAWNED)) {
			if(!itemHandler.getStackInSlot(getSizeInventory() - 1).isEmpty())
				return false;
			
			for(int i = 0; i < getSizeInventory(); i++) {
				if(itemHandler.getStackInSlot(i).isEmpty()) {
					itemHandler.setStackInSlot(i, stack.split(1));
					world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.BLOCKS, 0.1F, 10F);
					return true;
				}
			}
		}

		return false;
	}
	
	private ICustomApothecaryColor getFlowerComponent(ItemStack stack) {
		ICustomApothecaryColor c = null;
		if(stack.getItem() instanceof ICustomApothecaryColor)
			c = (ICustomApothecaryColor) stack.getItem();
		else if(stack.getItem() instanceof BlockItem && ((BlockItem) stack.getItem()).getBlock() instanceof ICustomApothecaryColor)
			c = (ICustomApothecaryColor) ((BlockItem) stack.getItem()).getBlock();
		
		return c;
	}

	public void saveLastRecipe() {
		lastRecipe = new ArrayList<>();
		for(int i = 0; i < getSizeInventory(); i++) {
			ItemStack stack = itemHandler.getStackInSlot(i);
			if(stack.isEmpty())
				break;
			lastRecipe.add(stack.copy());
		}
		recipeKeepTicks = 400;
		world.addBlockEvent(getPos(), getBlockState().getBlock(), SET_KEEP_TICKS_EVENT, 400);
	}

	public void trySetLastRecipe(PlayerEntity player) {
		tryToSetLastRecipe(player, itemHandler, lastRecipe);
		if(!isEmpty())
			VanillaPacketDispatcher.dispatchTEToNearbyPlayers(world, pos);
	}

	public static void tryToSetLastRecipe(PlayerEntity player, IItemHandlerModifiable inv, List<ItemStack> lastRecipe) {
		if(lastRecipe == null || lastRecipe.isEmpty() || player.world.isRemote)
			return;

		int index = 0;
		boolean didAny = false;
		for(ItemStack stack : lastRecipe) {
			if(stack.isEmpty())
				continue;

			for(int i = 0; i < player.inventory.getSizeInventory(); i++) {
				ItemStack pstack = player.inventory.getStackInSlot(i);
				if(!pstack.isEmpty() && pstack.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(stack, pstack)) {
					inv.setStackInSlot(index, pstack.split(1));
					didAny = true;
					index++;
					break;
				}
			}
		}

		if(didAny) {
			player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.BLOCKS, 0.1F, 10F);
			ServerPlayerEntity mp = (ServerPlayerEntity) player;
			mp.container.detectAndSendChanges();
		}
	}

	public boolean isEmpty() {
		for(int i = 0; i < getSizeInventory(); i++)
			if(!itemHandler.getStackInSlot(i).isEmpty())
				return false;

		return true;
	}

	@Override
	public void tick() {
		if(!world.isRemote) {
			List<ItemEntity> items = world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB(pos.add(0, 1D / 16D * 20D, 0), pos.add(1, 1D / 16D * 32D, 1)));

			boolean didChange = false;
			for(ItemEntity item : items)
				didChange = collideEntityItem(item) || didChange;

			if(didChange)
				VanillaPacketDispatcher.dispatchTEToNearbyPlayers(world, pos);
		} else {
			for(int i = 0; i < getSizeInventory(); i++) {
				ItemStack stackAt = itemHandler.getStackInSlot(i);
				if(stackAt.isEmpty())
					break;

				if(Math.random() >= 0.97) {
					ICustomApothecaryColor comp = getFlowerComponent(stackAt);
					Color color = new Color(comp == null ? 0x888888 : comp.getParticleColor(stackAt));
					float red = color.getRed() / 255F;
					float green = color.getGreen() / 255F;
					float blue = color.getBlue() / 255F;
					if(Math.random() >= 0.75F)
						world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.BLOCKS, 0.1F, 10F);
					Botania.proxy.sparkleFX(pos.getX() + 0.5 + Math.random() * 0.4 - 0.2, pos.getY() + 1.2, pos.getZ() + 0.5 + Math.random() * 0.4 - 0.2, red, green, blue, (float) Math.random(), 10);
				}
			}

			if(hasLava()) {
				world.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5 + Math.random() * 0.4 - 0.2, pos.getY() + 1, pos.getZ() + 0.5 + Math.random() * 0.4 - 0.2, 0, 0.05, 0);
				if(Math.random() > 0.9)
					world.addParticle(ParticleTypes.LAVA, pos.getX() + 0.5 + Math.random() * 0.4 - 0.2, pos.getY() + 1, pos.getZ() + 0.5 + Math.random() * 0.4 - 0.2, 0, 0.01, 0);
			}
		}

		if(recipeKeepTicks > 0)
			--recipeKeepTicks;
		else lastRecipe = null;
	}

	@Override
	public void writePacketNBT(CompoundNBT cmp) {
		super.writePacketNBT(cmp);

		cmp.putBoolean(TAG_HAS_WATER, hasWater());
		cmp.putBoolean(TAG_HAS_LAVA, hasLava());
	}

	@Override
	public void readPacketNBT(CompoundNBT cmp) {
		super.readPacketNBT(cmp);

		hasWater = cmp.getBoolean(TAG_HAS_WATER);
		hasLava = cmp.getBoolean(TAG_HAS_LAVA);
	}

	@Override
	public boolean receiveClientEvent(int id, int param) {
		switch(id) {
			case SET_KEEP_TICKS_EVENT: recipeKeepTicks = param; return true;
			case CRAFT_EFFECT_EVENT: {
				if(world.isRemote) {
					for(int i = 0; i < 25; i++) {
						float red = (float) Math.random();
						float green = (float) Math.random();
						float blue = (float) Math.random();
						Botania.proxy.sparkleFX(pos.getX() + 0.5 + Math.random() * 0.4 - 0.2, pos.getY() + 1, pos.getZ() + 0.5 + Math.random() * 0.4 - 0.2, red, green, blue, (float) Math.random(), 10);
					}
					world.playSound(pos.getX(), pos.getY(), pos.getZ(), ModSounds.altarCraft, SoundCategory.BLOCKS, 1F, 1F, false);
				}
				return true;
			}
			default: return super.receiveClientEvent(id, param);
		}
	}

	@Override
	public int getSizeInventory() {
		return 16;
	}

	@Override
	protected SimpleItemStackHandler createItemHandler() {
		return new SimpleItemStackHandler(this, false) {
			@Override
			protected int getStackLimit(int slot, @Nonnull ItemStack stack) {
				return 1;
			}
		};
	}

	@Override
	public void setWater(boolean water) {
		hasWater = water;
		VanillaPacketDispatcher.dispatchTEToNearbyPlayers(world, pos);
	}

	public void setLava(boolean lava) {
		hasLava = lava;
		VanillaPacketDispatcher.dispatchTEToNearbyPlayers(world, pos);
	}

	@Override
	public boolean hasWater() {
		return hasWater;
	}

	public boolean hasLava() {
		return hasLava;
	}

	@OnlyIn(Dist.CLIENT)
	public void renderHUD(Minecraft mc) {
		int xc = mc.mainWindow.getScaledWidth() / 2;
		int yc = mc.mainWindow.getScaledHeight() / 2;

		float angle = -90;
		int radius = 24;
		int amt = 0;
		for(int i = 0; i < getSizeInventory(); i++) {
			if(itemHandler.getStackInSlot(i).isEmpty())
				break;
			amt++;
		}

		if(amt > 0) {
			float anglePer = 360F / amt;

			for(RecipePetals recipe : BotaniaAPI.petalRecipes)
				if(recipe.matches(itemHandler)) {
					GlStateManager.color4f(1F, 1F, 1F, 1F);
					mc.textureManager.bindTexture(HUDHandler.manaBar);
					RenderHelper.drawTexturedModalRect(xc + radius + 9, yc - 8, 0, 0, 8, 22, 15);

					ItemStack stack = recipe.getOutput();

					net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
					mc.getItemRenderer().renderItemIntoGUI(stack, xc + radius + 32, yc - 8);
					mc.getItemRenderer().renderItemIntoGUI(new ItemStack(Items.WHEAT_SEEDS), xc + radius + 16, yc + 6);
					net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
					mc.fontRenderer.drawStringWithShadow("+", xc + radius + 14, yc + 10, 0xFFFFFF);
				}

			net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
			for(int i = 0; i < amt; i++) {
				double xPos = xc + Math.cos(angle * Math.PI / 180D) * radius - 8;
				double yPos = yc + Math.sin(angle * Math.PI / 180D) * radius - 8;
				GlStateManager.translated(xPos, yPos, 0);
				mc.getItemRenderer().renderItemIntoGUI(itemHandler.getStackInSlot(i), 0, 0);
				GlStateManager.translated(-xPos, -yPos, 0);

				angle += anglePer;
			}
			net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
		} else if(recipeKeepTicks > 0 && hasWater) {
			String s = I18n.format("botaniamisc.altarRefill0");
			mc.fontRenderer.drawStringWithShadow(s, xc - mc.fontRenderer.getStringWidth(s) / 2, yc + 10, 0xFFFFFF);
			s = I18n.format("botaniamisc.altarRefill1");
			mc.fontRenderer.drawStringWithShadow(s, xc - mc.fontRenderer.getStringWidth(s) / 2, yc + 20, 0xFFFFFF);
		}
	}

}
