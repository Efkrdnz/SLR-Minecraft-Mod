package net.solocraft.init;

import net.solocraft.client.model.Modelshardparticle_Converted;
import net.solocraft.client.model.Modelshalegs;
import net.solocraft.client.model.Modelshahed;
import net.solocraft.client.model.Modelshadowtorso;
import net.solocraft.client.model.Modelshadowsoul;
import net.solocraft.client.model.Modelshadowlegs;
import net.solocraft.client.model.Modelshadowhead;
import net.solocraft.client.model.Modelshadowfeet;
import net.solocraft.client.model.Modelshaces;
import net.solocraft.client.model.Modelshabots;
import net.solocraft.client.model.Modelmanaarrow;
import net.solocraft.client.model.Modellight_ball;
import net.solocraft.client.model.Modelkangtaeshikhair;
import net.solocraft.client.model.Modelkangtaeshik;
import net.solocraft.client.model.Modelkang;
import net.solocraft.client.model.Modeljinwoolegs2;
import net.solocraft.client.model.Modeljinwoolegs1;
import net.solocraft.client.model.Modeljinwoochest2;
import net.solocraft.client.model.Modeljinwoochest1;
import net.solocraft.client.model.Modelinv;
import net.solocraft.client.model.Modelicecle;
import net.solocraft.client.model.Modelchoicloak;
import net.solocraft.client.model.ModelSlash6;
import net.solocraft.client.model.ModelSlash5;
import net.solocraft.client.model.ModelSlash4;
import net.solocraft.client.model.ModelSlash3;
import net.solocraft.client.model.ModelSlash2;
import net.solocraft.client.model.ModelSlash;
import net.solocraft.client.model.ModelFlameArrow;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.api.distmarker.Dist;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = {Dist.CLIENT})
public class SololevelingModModels {
	@SubscribeEvent
	public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(Modelshadowlegs.LAYER_LOCATION, Modelshadowlegs::createBodyLayer);
		event.registerLayerDefinition(Modelshardparticle_Converted.LAYER_LOCATION, Modelshardparticle_Converted::createBodyLayer);
		event.registerLayerDefinition(ModelSlash6.LAYER_LOCATION, ModelSlash6::createBodyLayer);
		event.registerLayerDefinition(Modelshalegs.LAYER_LOCATION, Modelshalegs::createBodyLayer);
		event.registerLayerDefinition(Modelshadowsoul.LAYER_LOCATION, Modelshadowsoul::createBodyLayer);
		event.registerLayerDefinition(ModelSlash4.LAYER_LOCATION, ModelSlash4::createBodyLayer);
		event.registerLayerDefinition(Modeljinwoochest2.LAYER_LOCATION, Modeljinwoochest2::createBodyLayer);
		event.registerLayerDefinition(Modelicecle.LAYER_LOCATION, Modelicecle::createBodyLayer);
		event.registerLayerDefinition(Modelkang.LAYER_LOCATION, Modelkang::createBodyLayer);
		event.registerLayerDefinition(Modeljinwoolegs1.LAYER_LOCATION, Modeljinwoolegs1::createBodyLayer);
		event.registerLayerDefinition(ModelSlash2.LAYER_LOCATION, ModelSlash2::createBodyLayer);
		event.registerLayerDefinition(Modeljinwoochest1.LAYER_LOCATION, Modeljinwoochest1::createBodyLayer);
		event.registerLayerDefinition(Modelkangtaeshikhair.LAYER_LOCATION, Modelkangtaeshikhair::createBodyLayer);
		event.registerLayerDefinition(Modelinv.LAYER_LOCATION, Modelinv::createBodyLayer);
		event.registerLayerDefinition(Modelshaces.LAYER_LOCATION, Modelshaces::createBodyLayer);
		event.registerLayerDefinition(Modelmanaarrow.LAYER_LOCATION, Modelmanaarrow::createBodyLayer);
		event.registerLayerDefinition(Modelshabots.LAYER_LOCATION, Modelshabots::createBodyLayer);
		event.registerLayerDefinition(Modelshadowtorso.LAYER_LOCATION, Modelshadowtorso::createBodyLayer);
		event.registerLayerDefinition(ModelSlash3.LAYER_LOCATION, ModelSlash3::createBodyLayer);
		event.registerLayerDefinition(Modelkangtaeshik.LAYER_LOCATION, Modelkangtaeshik::createBodyLayer);
		event.registerLayerDefinition(ModelSlash.LAYER_LOCATION, ModelSlash::createBodyLayer);
		event.registerLayerDefinition(Modelshadowfeet.LAYER_LOCATION, Modelshadowfeet::createBodyLayer);
		event.registerLayerDefinition(Modeljinwoolegs2.LAYER_LOCATION, Modeljinwoolegs2::createBodyLayer);
		event.registerLayerDefinition(ModelSlash5.LAYER_LOCATION, ModelSlash5::createBodyLayer);
		event.registerLayerDefinition(Modelshahed.LAYER_LOCATION, Modelshahed::createBodyLayer);
		event.registerLayerDefinition(Modellight_ball.LAYER_LOCATION, Modellight_ball::createBodyLayer);
		event.registerLayerDefinition(Modelchoicloak.LAYER_LOCATION, Modelchoicloak::createBodyLayer);
		event.registerLayerDefinition(Modelshadowhead.LAYER_LOCATION, Modelshadowhead::createBodyLayer);
		event.registerLayerDefinition(ModelFlameArrow.LAYER_LOCATION, ModelFlameArrow::createBodyLayer);
	}
}
