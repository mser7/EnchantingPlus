package eplus.common;

import net.minecraft.src.BlockEnchantmentTable;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.TileEntity;
import net.minecraft.src.TileEntityEnchantmentTable;
import net.minecraft.src.World;

public class BlockEnchantingTable extends BlockEnchantmentTable
{

    protected BlockEnchantingTable(int par1)
    {
        super(par1);
    }

    @Override
    public TileEntity createNewTileEntity(World par1World)
    {
        return new TileEntityEnchantmentTable();
    }

    public boolean onBlockActivated(World var1, int var2, int var3, int var4, EntityPlayer var5, int var6, float var7, float var8, float var9)
    {
        if (var1.isRemote)
        {
            return true;
        }
        else
        {
            if (EnchantingPlus.useMod)
            {
                var5.openGui(EnchantingPlus.instance, 0, var1, var2, var3, var4);
            }
            else
            {
                var5.displayGUIEnchantment(var2, var3, var4);
            }
            return true;
        }
    }

}
