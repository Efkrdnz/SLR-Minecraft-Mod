package net.solocraft.mixins;

import net.solocraft.worldgen.VillageUtilityStructureInjector;

import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Adds the utility buildings at the owning structure boundary instead of
 * inside {@code JigsawPlacement}. Loquat can short-circuit that method with a
 * replacement generation stub, while Waystones rewrites its child-pool
 * lookups. Staying outside that implementation detail lets both mods finish
 * their generation changes before we append our pieces.
 */
@Mixin(JigsawStructure.class)
public abstract class VillageJigsawPlacementMixin {
	@Shadow
	@Final
	private Holder<StructureTemplatePool> startPool;

	@Inject(
			method = "findGenerationPoint(Lnet/minecraft/world/level/levelgen/structure/Structure$GenerationContext;)Ljava/util/Optional;",
			at = @At("RETURN"),
			cancellable = true)
	private void sololeveling$addVillageUtilities(
			Structure.GenerationContext context,
			CallbackInfoReturnable<Optional<Structure.GenerationStub>> callback) {
		callback.setReturnValue(VillageUtilityStructureInjector.inject(context,
				this.startPool, callback.getReturnValue()));
	}
}
