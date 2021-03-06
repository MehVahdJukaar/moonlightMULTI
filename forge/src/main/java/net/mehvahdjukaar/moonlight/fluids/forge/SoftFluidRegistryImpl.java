package net.mehvahdjukaar.moonlight.fluids.forge;

import net.mehvahdjukaar.moonlight.Moonlight;
import net.mehvahdjukaar.moonlight.fluids.SoftFluid;
import net.mehvahdjukaar.moonlight.fluids.SoftFluidRegistry;
import net.mehvahdjukaar.moonlight.util.Utils;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.*;

import java.util.HashMap;
import java.util.function.Supplier;

public class SoftFluidRegistryImpl {

    public static final ResourceKey<Registry<SoftFluid>> KEY = SoftFluidRegistry.REGISTRY_KEY;

    public static final DeferredRegister<SoftFluid> DEFERRED_REGISTER = DeferredRegister.create(KEY, KEY.location().getNamespace());
    public static final Supplier<IForgeRegistry<SoftFluidImpl>> SOFT_FLUIDS = DEFERRED_REGISTER.makeRegistry(() ->
            new RegistryBuilder<SoftFluidImpl>()
                    .setDefaultKey(Moonlight.res("empty"))
                    .dataPackRegistry(SoftFluid.CODEC, SoftFluid.CODEC)
                    .onCreate(SoftFluidRegistryImpl::onCreate)
                    .onClear(SoftFluidRegistryImpl::onClear)
                    .allowModification()
                    .disableSaving());

    public static HashMap<Fluid, SoftFluid> getFluidsMap() {
        return SOFT_FLUIDS.get().getSlaveMap(FLUIDS_MAP_KEY, HashMap.class);
    }

    public static HashMap<Item, SoftFluid> getItemsMap() {
        return SOFT_FLUIDS.get().getSlaveMap(ITEMS_MAP_KEY, HashMap.class);
    }

    public static void onCreate(IForgeRegistryInternal<SoftFluidImpl> owner, RegistryManager stage) {
        owner.setSlaveMap(FLUIDS_MAP_KEY, new HashMap<>());
        owner.setSlaveMap(ITEMS_MAP_KEY, new HashMap<>());
    }

    public static void onClear(IForgeRegistryInternal<SoftFluidImpl> owner, RegistryManager stage) {
        owner.getSlaveMap(FLUIDS_MAP_KEY, HashMap.class).clear();
        owner.getSlaveMap(ITEMS_MAP_KEY, HashMap.class).clear();
    }

    public static void addExistingForgeFluids() {
        //only runs on the first object
        var fluidMap = getFluidsMap();
        MappedRegistry<SoftFluid> reg = (MappedRegistry<SoftFluid>) getDataPackRegistry();
        reg.unfreeze();
        for (Fluid f : ForgeRegistries.FLUIDS) {
            try {
                if (f == null) continue;
                if (f instanceof FlowingFluid flowingFluid && flowingFluid.getSource() != f) continue;
                if (f instanceof ForgeFlowingFluid.Flowing || f == Fluids.EMPTY) continue;
                //if fluid map contains fluid it means that another equivalent fluid has already been registered
                if (fluidMap.containsKey(f)) continue;
                //is not equivalent: create new SoftFluid from forge fluid
                if (Utils.getID(f) != null) {
                    SoftFluid sf = (new SoftFluid.Builder(f)).build();
                    //calling vanilla register function because calling that deferred register or forge registry now does nothing
                    //cope
                    //SOFT_FLUIDS.get().register(sf.getRegistryName(),sf);
                    Registry.register(reg, Utils.getID(f), sf);
                    fluidMap.put(f, sf);
                }
            } catch (Exception ignored) {
            }
        }
        //adds empty fluid
        Registry.register(reg, Moonlight.res("empty"), EMPTY);
        reg.freeze();
    }


}
