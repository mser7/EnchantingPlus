package net.epoxide.eplus.inventory;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import net.darkhax.bookshelf.lib.util.Utilities;
import net.epoxide.eplus.handler.ContentHandler;
import net.epoxide.eplus.handler.EPlusConfigurationHandler;
import net.epoxide.eplus.tileentity.TileEntityEnchantTable;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;

public class ContainerEnchantTable extends Container {
    
    public final World world;
    private final TileEntityEnchantTable tileEnchantTable;
    private final int x;
    private final int y;
    private final int z;
    private final EntityPlayer player;
    private Map<Integer, Integer> enchantments = new HashMap<Integer, Integer>();
    public final IInventory tableInventory = new SlotEnchantTable(this, "Enchant", true, 1);
    
    public ContainerEnchantTable(final InventoryPlayer inventoryPlayer, World world, int x, int y, int z, TileEntityEnchantTable tileEntityTable) {
        
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        
        this.tileEnchantTable = tileEntityTable;
        
        this.player = inventoryPlayer.player;
        
        int guiOffest = 26;
        
        addSlotToContainer(new SlotEnchant(this, tableInventory, 0, 11 + guiOffest, 17));
        
        for (int l = 0; l < 3; ++l) {
            for (int i1 = 0; i1 < 9; ++i1) {
                addSlotToContainer(new Slot(inventoryPlayer, i1 + l * 9 + 9, 17 + i1 * 18 + guiOffest, 91 + l * 18));
            }
        }
        
        for (int l = 0; l < 9; ++l) {
            addSlotToContainer(new Slot(inventoryPlayer, l, 17 + l * 18 + guiOffest, 149));
        }
        
        for (int k = 0; k < 4; k++) {
            final int armorType = k;
            addSlotToContainer(new Slot(inventoryPlayer, 39 - k, 7, 24 + k * 19) {
                @Override
                public int getSlotStackLimit () {
                    
                    return 1;
                }
                
                @Override
                public boolean isItemValid (ItemStack par1ItemStack) {
                    
                    Item item = (par1ItemStack == null ? null : par1ItemStack.getItem());
                    return item != null && item.isValidArmor(par1ItemStack, armorType, player);
                }
            });
        }
        
        if (tileEnchantTable.itemInTable != null) {
            player.entityDropItem(tileEnchantTable.itemInTable, 0.2f);
            tileEnchantTable.itemInTable.stackSize = 0;
        }
    }
    
    public float bookCases () {
        
        float temp = EPlusConfigurationHandler.minimumBookshelfs;
        for (int j = -1; j <= 1; ++j) {
            for (int k = -1; k <= 1; ++k) {
                if ((j != 0 || k != 0) && world.isAirBlock(x + k, y, y + j) && world.isAirBlock(x + k, y + 1, z + j)) {
                    temp += ForgeHooks.getEnchantPower(world, x + k * 2, y, z + j * 2);
                    temp += ForgeHooks.getEnchantPower(world, x + k * 2, y + 1, z + j * 2);
                    
                    if (k != 0 && j != 0) {
                        temp += ForgeHooks.getEnchantPower(world, x + k * 2, y, z + j);
                        temp += ForgeHooks.getEnchantPower(world, x + k * 2, y + 1, z + j);
                        temp += ForgeHooks.getEnchantPower(world, x + k, y, z + j * 2);
                        temp += ForgeHooks.getEnchantPower(world, x + k, y + 1, z + j * 2);
                    }
                }
            }
        }
        
        return temp * 2;
    }
    
    @Override
    public boolean canInteractWith (EntityPlayer entityPlayer) {
        
        return entityPlayer.getDistanceSq((double) this.x + 0.5D, (double) this.y + 0.5D, (double) this.z + 0.5D) <= 64.0D && !entityPlayer.isDead;
    }
    
    public boolean canPurchase (EntityPlayer player, int cost) {
        
        if (player.capabilities.isCreativeMode) {
            return true;
        }
        
        if (EPlusConfigurationHandler.needsBookShelves) {
            if (cost > bookCases()) {
                player.addChatMessage(new ChatComponentText("Not enough bookcases. Required " + cost));
                return false;
            }
        }
        
        if (player.experienceLevel < cost) {
            player.addChatMessage(new ChatComponentText("Not enough levels. Required " + cost));
            return false;
        }
        return true;
    }
    
    public int disenchantmentCost (Enchantment enchantment, int enchantmentLevel, Integer level) {
        
        final ItemStack itemStack = tableInventory.getStackInSlot(0);
        if (itemStack == null)
            return 0;
            
        final int maxLevel = enchantment.getMaxLevel();
        
        if (enchantmentLevel > maxLevel)
            return 0;
            
        final int averageCost = (enchantment.getMinEnchantability(level) + enchantment.getMaxEnchantability(level)) / 2;
        int enchantability = itemStack.getItem().getItemEnchantability(itemStack);
        
        if (enchantability <= 1)
            enchantability = 10;
            
        int adjustedCost = (int) (averageCost * (enchantmentLevel - level - maxLevel) / ((double) maxLevel * enchantability));
        if (!EPlusConfigurationHandler.needsBookShelves) {
            int temp = (int) (adjustedCost * (60 / (bookCases() + 1)));
            temp /= 20;
            if (temp > adjustedCost) {
                adjustedCost = temp;
            }
        }
        
        adjustedCost *= (EPlusConfigurationHandler.costFactor / 4D);
        
        if (enchantability > 1)
            adjustedCost *= Math.log(enchantability) / 2;
        else
            adjustedCost /= 10;
            
        final int enchantmentCost = enchantmentCost(enchantment, level - 1, enchantmentLevel);
        
        return Math.min(adjustedCost, -enchantmentCost);
    }
    
    /**
     * Enchants an item
     *
     * @param player player requesting the enchantment
     * @param map the list of enchantments to add
     * @param levels
     * @param cost the cost of the operation
     * @throws Exception
     */
    public void enchant (EntityPlayer player, HashMap<Integer, Integer> map, HashMap<Integer, Integer> levels, int cost) throws Exception {
        
        final ItemStack itemstack = tableInventory.getStackInSlot(0);
        final HashMap<Integer, Integer> temp = new HashMap<Integer, Integer>();
        int serverCost = 0;
        
        if (itemstack == null)
            return;
            
        for (final Integer enchantId : map.keySet()) {
            final Integer level = map.get(enchantId);
            final Integer startingLevel = enchantments.get(enchantId);
            Enchantment enchantment = Utilities.getEnchantment(enchantId);
            if (level > startingLevel)
                serverCost += enchantmentCost(enchantment, level, startingLevel);
            else if (level < startingLevel)
                serverCost += disenchantmentCost(enchantment, level, startingLevel);
        }
        
        if (cost != serverCost) {
            throw new Exception("Cost is different on client and server");
        }
        
        for (final Integer enchantId : enchantments.keySet()) {
            final Integer level = enchantments.get(enchantId);
            
            if (level != 0) {
                if (!map.containsKey(enchantId)) {
                    map.put(enchantId, level);
                }
            }
        }
        
        for (final Integer enchantId : map.keySet()) {
            final Integer level = map.get(enchantId);
            
            if (level == 0)
                temp.put(enchantId, level);
                
        }
        for (Integer object : temp.keySet()) {
            map.remove(object);
        }
        
        if (canPurchase(player, serverCost)) {
            ItemStack itemStack = EnchantHelper.setEnchantments(map, itemstack, levels, player);
            tableInventory.setInventorySlotContents(0, itemStack);
            if (!player.capabilities.isCreativeMode)
                player.addExperienceLevel(-cost);
        }
        
        onCraftMatrixChanged(tableInventory);
        
    }
    
    public int enchantmentCost (Enchantment enchantment, int enchantmentLevel, Integer level) {
        
        final ItemStack itemStack = tableInventory.getStackInSlot(0);
        if (itemStack == null)
            return 0;
            
        final int maxLevel = enchantment.getMaxLevel();
        
        if (enchantmentLevel > maxLevel) {
            return 0;
        }
        
        final int averageCost = (enchantment.getMinEnchantability(enchantmentLevel) + enchantment.getMaxEnchantability(enchantmentLevel)) / 2;
        int enchantability = itemStack.getItem().getItemEnchantability();
        
        if (enchantability < 1)
            enchantability = 1;
            
        int adjustedCost = (int) (averageCost * (enchantmentLevel - level + maxLevel) / ((double) maxLevel * enchantability));
        
        if (!EPlusConfigurationHandler.needsBookShelves) {
            int temp = (int) (adjustedCost * (60 / (bookCases() + 1)));
            temp /= 20;
            if (temp > adjustedCost) {
                adjustedCost = temp;
            }
        }
        
        adjustedCost *= (EPlusConfigurationHandler.costFactor / 3D);
        if (enchantability > 1) {
            adjustedCost *= Math.log(enchantability) / 2;
        }
        else {
            adjustedCost /= 10;
        }
        
        return Math.max(1, adjustedCost);
    }
    
    public Map<Integer, Integer> getEnchantments () {
        
        return enchantments;
    }
    
    @Override
    public void onContainerClosed (EntityPlayer par1EntityPlayer) {
        
        super.onContainerClosed(par1EntityPlayer);
        
        for (int i = 0; i < tableInventory.getSizeInventory(); i++) {
            final ItemStack stack = tableInventory.getStackInSlot(i);
            if (stack != null) {
                if (!par1EntityPlayer.inventory.addItemStackToInventory(stack)) {
                    par1EntityPlayer.entityDropItem(stack, 0.2f);
                }
            }
        }
    }
    
    @Override
    public void onCraftMatrixChanged (IInventory par1IInventory) {
        
        super.onCraftMatrixChanged(par1IInventory);
        
        tileEnchantTable.getWorldObj().markBlockForUpdate(tileEnchantTable.xCoord, tileEnchantTable.yCoord, tileEnchantTable.zCoord);
        readItems();
    }
    
    /**
     * Will read the enchantments on the items and ones the can be added to the items
     */
    private void readItems () {
        
        final ItemStack itemStack = tableInventory.getStackInSlot(0);
        
        final HashMap<Integer, Integer> temp = new LinkedHashMap<Integer, Integer>();
        final HashMap<Integer, Integer> temp2 = new LinkedHashMap<Integer, Integer>();
        
        if (itemStack != null && !ContentHandler.isBlacklisted(itemStack.getItem())) {
            if (EnchantHelper.isItemEnchantable(itemStack)) {
                addEnchantsFor(itemStack, temp);
            }
            else if (EnchantHelper.isItemEnchanted(itemStack) && EnchantHelper.isNewItemEnchantable(itemStack.getItem())) {
                temp.putAll(EnchantmentHelper.getEnchantments(itemStack));
                
                for (final Enchantment obj : Enchantment.enchantmentsList) {
                    if (obj == null)
                        continue;
                        
                    boolean add = true;
                    for (final Integer enc : temp.keySet()) {
                        
                        final Enchantment enchantment = Utilities.getEnchantment(enc);
                        if (enchantment == null)
                            continue;
                            
                        if (!EnchantHelper.isEnchantmentsCompatible(enchantment, obj)) {
                            add = false;
                        }
                    }
                    if (add) {
                        addEnchantFor(itemStack, temp2, obj);
                    }
                }
                temp.putAll(temp2);
            }
            
            if (enchantments != temp) {
                enchantments = temp;
            }
        }
        else {
            enchantments = temp;
        }
    }
    
    private void addEnchantsFor (ItemStack itemStack, HashMap<Integer, Integer> temp) {
        
        for (final Enchantment obj : Enchantment.enchantmentsList) {
            addEnchantFor(itemStack, temp, obj);
        }
    }
    
    private void addEnchantFor (ItemStack itemStack, HashMap<Integer, Integer> temp, Enchantment obj) {
        
        if (EnchantHelper.isEnchantmentValid(obj, player) && !ContentHandler.isBlacklisted(obj) && obj.canApplyAtEnchantingTable(itemStack)) {
            temp.put(obj.effectId, 0);
        }
    }
    
    public void repair (EntityPlayer player, int cost) throws Exception {
        
        final ItemStack itemStack = tableInventory.getStackInSlot(0);
        
        if (itemStack == null)
            return;
            
        boolean flag = !itemStack.hasTagCompound() || !itemStack.getTagCompound().hasKey("charge");
        
        if ((!itemStack.isItemEnchanted() || cost == 0) && flag)
            return;
            
        if (canPurchase(player, cost)) {
            int maxCost = repairCostMax();
            double percAmnt = cost / (double) maxCost;
            
            int remain = itemStack.getItemDamageForDisplay();
            double newDamage = remain - remain * percAmnt;
            newDamage = (newDamage <= 0) ? 0 : newDamage;
            
            itemStack.setItemDamage((int) newDamage);
            if (!player.capabilities.isCreativeMode)
                player.addExperienceLevel(-cost);
                
        }
        
        onCraftMatrixChanged(tableInventory);
    }
    
    public int repairCostMax () {
        
        final ItemStack itemStack = tableInventory.getStackInSlot(0);
        if (itemStack == null)
            return 0;
            
        if (!itemStack.isItemEnchanted() || !itemStack.isItemDamaged())
            return 0;
            
        int cost = 0;
        
        final Map<Integer, Integer> enchantments = EnchantmentHelper.getEnchantments(itemStack);
        
        for (final Integer enchantment : enchantments.keySet()) {
            final Integer enchantmentLevel = enchantments.get(enchantment);
            
            cost += enchantmentCost(Utilities.getEnchantment(enchantment), enchantmentLevel, 0);
        }
        
        final int maxDamage = itemStack.getMaxDamage();
        final int displayDamage = itemStack.getItemDamageForDisplay();
        int enchantability = itemStack.getItem().getItemEnchantability(itemStack);
        
        if (enchantability <= 1) {
            enchantability = 10;
        }
        
        final double percentDamage = 1 - (maxDamage - displayDamage) / (double) maxDamage;
        
        double totalCost = (percentDamage * cost) / enchantability;
        
        totalCost *= 2 * EPlusConfigurationHandler.repairFactor;
        
        return (int) Math.max(1, totalCost);
    }
    
    @Override
    public ItemStack transferStackInSlot (EntityPlayer entityPlayer, int par2) {
        
        ItemStack itemStack = null;
        final Slot slot = (Slot) inventorySlots.get(par2);
        
        if (slot != null && slot.getHasStack()) {
            final ItemStack stack = slot.getStack();
            
            final ItemStack tempStack = stack.copy();
            itemStack = stack.copy();
            tempStack.stackSize = 1;
            
            if (par2 != 0) {
                final Slot slot1 = (Slot) inventorySlots.get(0);
                
                if (!slot1.getHasStack() && slot1.isItemValid(tempStack) && mergeItemStack(tempStack, 0, 1, false)) {
                    stack.stackSize--;
                    itemStack = stack.copy();
                }
                
            }
            else if (!mergeItemStack2(stack)) {
                return null;
            }
            
            if (stack.stackSize == 0) {
                slot.putStack(null);
            }
            else {
                slot.onSlotChanged();
            }
            
            if (itemStack.stackSize == stack.stackSize) {
                return null;
            }
            slot.onPickupFromSlot(entityPlayer, stack);
        }
        return itemStack;
    }
    
    private boolean mergeItemStack2 (ItemStack stack) {
        
        boolean flag1 = false;
        int k;
        
        k = 41 - 1;
        
        Slot slot;
        ItemStack itemstack1;
        
        if (stack.isStackable()) {
            while (stack.stackSize > 0 && (k >= 1)) {
                slot = (Slot) this.inventorySlots.get(k);
                itemstack1 = slot.getStack();
                
                if (itemstack1 != null && itemstack1 == stack && (!stack.getHasSubtypes() || stack.getItemDamage() == itemstack1.getItemDamage()) && ItemStack.areItemStackTagsEqual(stack, itemstack1)) {
                    int l = itemstack1.stackSize + stack.stackSize;
                    
                    if (l <= stack.getMaxStackSize()) {
                        stack.stackSize = 0;
                        itemstack1.stackSize = l;
                        slot.onSlotChanged();
                        flag1 = true;
                    }
                    else if (itemstack1.stackSize < stack.getMaxStackSize()) {
                        stack.stackSize -= stack.getMaxStackSize() - itemstack1.stackSize;
                        itemstack1.stackSize = stack.getMaxStackSize();
                        slot.onSlotChanged();
                        flag1 = true;
                    }
                }
                
                --k;
            }
        }
        
        if (stack.stackSize > 0) {
            k = 41 - 1;
            
            while (k >= 1) {
                slot = (Slot) this.inventorySlots.get(k);
                itemstack1 = slot.getStack();
                
                if (itemstack1 == null && slot.isItemValid(stack)) {
                    slot.putStack(stack.copy());
                    slot.onSlotChanged();
                    stack.stackSize = 0;
                    flag1 = true;
                    break;
                }
                
                --k;
            }
        }
        
        return flag1;
    }
}
