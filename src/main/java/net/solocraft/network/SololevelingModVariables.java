package net.solocraft.network;

import org.checkerframework.checker.units.qual.Speed;

import net.solocraft.SololevelingMod;

import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.Capability;

import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraft.client.Minecraft;

import joptsimple.internal.Classes;

import java.util.function.Supplier;
import java.util.List;
import java.util.ArrayList;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class SololevelingModVariables {
	public static List<Object> parties = new ArrayList<>();
	public static List<Object> partypassword = new ArrayList<>();

	@SubscribeEvent
	public static void init(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(SavedDataSyncMessage.class, SavedDataSyncMessage::buffer, SavedDataSyncMessage::new, SavedDataSyncMessage::handler);
		SololevelingMod.addNetworkMessage(PlayerVariablesSyncMessage.class, PlayerVariablesSyncMessage::buffer, PlayerVariablesSyncMessage::new, PlayerVariablesSyncMessage::handler);
	}

	@SubscribeEvent
	public static void init(RegisterCapabilitiesEvent event) {
		event.register(PlayerVariables.class);
	}

	@Mod.EventBusSubscriber
	public static class EventBusVariableHandlers {
		@SubscribeEvent
		public static void onPlayerLoggedInSyncPlayerVariables(PlayerEvent.PlayerLoggedInEvent event) {
			if (!event.getEntity().level().isClientSide()) {
				for (Entity entityiterator : new ArrayList<>(event.getEntity().level().players())) {
					((PlayerVariables) entityiterator.getCapability(PLAYER_VARIABLES_CAPABILITY, null).orElse(new PlayerVariables())).syncPlayerVariables(entityiterator);
				}
			}
		}

		@SubscribeEvent
		public static void onPlayerRespawnedSyncPlayerVariables(PlayerEvent.PlayerRespawnEvent event) {
			if (!event.getEntity().level().isClientSide()) {
				for (Entity entityiterator : new ArrayList<>(event.getEntity().level().players())) {
					((PlayerVariables) entityiterator.getCapability(PLAYER_VARIABLES_CAPABILITY, null).orElse(new PlayerVariables())).syncPlayerVariables(entityiterator);
				}
			}
		}

		@SubscribeEvent
		public static void onPlayerChangedDimensionSyncPlayerVariables(PlayerEvent.PlayerChangedDimensionEvent event) {
			if (!event.getEntity().level().isClientSide()) {
				for (Entity entityiterator : new ArrayList<>(event.getEntity().level().players())) {
					((PlayerVariables) entityiterator.getCapability(PLAYER_VARIABLES_CAPABILITY, null).orElse(new PlayerVariables())).syncPlayerVariables(entityiterator);
				}
			}
		}

		@SubscribeEvent
		public static void clonePlayer(PlayerEvent.Clone event) {
			event.getOriginal().revive();
			PlayerVariables original = ((PlayerVariables) event.getOriginal().getCapability(PLAYER_VARIABLES_CAPABILITY, null).orElse(new PlayerVariables()));
			PlayerVariables clone = ((PlayerVariables) event.getEntity().getCapability(PLAYER_VARIABLES_CAPABILITY, null).orElse(new PlayerVariables()));
			clone.shopitem5 = original.shopitem5;
			clone.shopitem1 = original.shopitem1;
			clone.shopitem2 = original.shopitem2;
			clone.shopitem3 = original.shopitem3;
			clone.shopitem4 = original.shopitem4;
			clone.shopitem6 = original.shopitem6;
			clone.Ab1 = original.Ab1;
			clone.Ab2 = original.Ab2;
			clone.Ab3 = original.Ab3;
			clone.Ab4 = original.Ab4;
			clone.abilities = original.abilities;
			clone.ActiveDaily = original.ActiveDaily;
			clone.alivestatus = original.alivestatus;
			clone.beru = original.beru;
			clone.berumax = original.berumax;
			clone.boss = original.boss;
			clone.BossKilled = original.BossKilled;
			clone.Call4Death = original.Call4Death;
			clone.Classes = original.Classes;
			clone.combatmode = original.combatmode;
			clone.commanddeath = original.commanddeath;
			clone.dailykilltyppe = original.dailykilltyppe;
			clone.dailysecrettrans = original.dailysecrettrans;
			clone.dailytasks = original.dailytasks;
			clone.dailytimer = original.dailytimer;
			clone.DeathX = original.DeathX;
			clone.DeathY = original.DeathY;
			clone.DeathZ = original.DeathZ;
			clone.Dialogue = original.Dialogue;
			clone.domain = original.domain;
			clone.dungeoning = original.dungeoning;
			clone.DungeonNum = original.DungeonNum;
			clone.DunX = original.DunX;
			clone.DunY = original.DunY;
			clone.DunZ = original.DunZ;
			clone.Durability = original.Durability;
			clone.giftstatus = original.giftstatus;
			clone.GobShadow = original.GobShadow;
			clone.GobShadowMax = original.GobShadowMax;
			clone.golds = original.golds;
			clone.guardbar = original.guardbar;
			clone.GuildCode = original.GuildCode;
			clone.HunterEyes = original.HunterEyes;
			clone.HunterRank = original.HunterRank;
			clone.igris = original.igris;
			clone.IgrisSpawned = original.IgrisSpawned;
			clone.Intelligence = original.Intelligence;
			clone.investvalue = original.investvalue;
			clone.JOB = original.JOB;
			clone.jobkey = original.jobkey;
			clone.killmission = original.killmission;
			clone.LastKilled = original.LastKilled;
			clone.Level = original.Level;
			clone.MainQuest = original.MainQuest;
			clone.manaregen = original.manaregen;
			clone.MaxXP = original.MaxXP;
			clone.orcmax = original.orcmax;
			clone.orcspawned = original.orcspawned;
			clone.OrdShadow = original.OrdShadow;
			clone.ordshadowmax = original.ordshadowmax;
			clone.overridefeet = original.overridefeet;
			clone.overridehead = original.overridehead;
			clone.overridelegs = original.overridelegs;
			clone.overridetorso = original.overridetorso;
			clone.perception = original.perception;
			clone.Player = original.Player;
			clone.polarbear = original.polarbear;
			clone.polarbearmax = original.polarbearmax;
			clone.pushup = original.pushup;
			clone.ranking = original.ranking;
			clone.rankingnum = original.rankingnum;
			clone.resistance = original.resistance;
			clone.RUN = original.RUN;
			clone.RX = original.RX;
			clone.RZ = original.RZ;
			clone.ShadowExchange = original.ShadowExchange;
			clone.ShadowSelect = original.ShadowSelect;
			clone.shadowstorage = original.shadowstorage;
			clone.shadowstorageusage = original.shadowstorageusage;
			clone.situp = original.situp;
			clone.SkillPoints = original.SkillPoints;
			clone.slashfury = original.slashfury;
			clone.Speed = original.Speed;
			clone.speedpercent = original.speedpercent;
			clone.squat = original.squat;
			clone.statshown = original.statshown;
			clone.Strength = original.Strength;
			clone.summonlimit = original.summonlimit;
			clone.summonlimitusage = original.summonlimitusage;
			clone.tj = original.tj;
			clone.tjonoff = original.tjonoff;
			clone.Vitality = original.Vitality;
			clone.WolfShadow = original.WolfShadow;
			clone.WolfShadowMax = original.WolfShadowMax;
			clone.Xp = original.Xp;
			clone.xpmultiplier = original.xpmultiplier;
			clone.Money = original.Money;
			clone.CustomHUD = original.CustomHUD;
			clone.ShadowGoblinArcherAmount = original.ShadowGoblinArcherAmount;
			clone.ShadowGoblinMageAmount = original.ShadowGoblinMageAmount;
			clone.ShadowGoblinArcherMax = original.ShadowGoblinArcherMax;
			clone.ShadowGoblinMageMax = original.ShadowGoblinMageMax;
			clone.shadowdragonnum = original.shadowdragonnum;
			clone.shadowdragonmax = original.shadowdragonmax;
			clone.packetCounter = original.packetCounter;
			clone.daily_refreshes = original.daily_refreshes;
			clone.selection = original.selection;
			clone.party = original.party;
			clone.prevRank = original.prevRank;
			clone.prevLevel = original.prevLevel;
			clone.idcd = original.idcd;
			clone.title = original.title;
			clone.Plist = original.Plist;
			clone.Pslot1 = original.Pslot1;
			clone.Pslot2 = original.Pslot2;
			clone.Pslot3 = original.Pslot3;
			clone.Pslot4 = original.Pslot4;
			clone.Pslot5 = original.Pslot5;
			clone.Pslot6 = original.Pslot6;
			clone.Pslot7 = original.Pslot7;
			clone.Pslot8 = original.Pslot8;
			clone.PselectedPower = original.PselectedPower;
			clone.progression_assassin = original.progression_assassin;
			clone.progression_mage = original.progression_mage;
			clone.progression_fighter = original.progression_fighter;
			clone.progression_tanker = original.progression_tanker;
			clone.progression_healer = original.progression_healer;
			clone.progression_ranger = original.progression_ranger;
			clone.JobSkills = original.JobSkills;
			clone.ExchangeDimensions = original.ExchangeDimensions;
			clone.ExchangeCords = original.ExchangeCords;
			clone.ShadowBody = original.ShadowBody;
			clone.progression_multiplier_assassin = original.progression_multiplier_assassin;
			clone.progression_multiplier_mage = original.progression_multiplier_mage;
			clone.progression_multiplier_fighter = original.progression_multiplier_fighter;
			clone.progression_multiplier_tanker = original.progression_multiplier_tanker;
			clone.progression_multiplier_healer = original.progression_multiplier_healer;
			clone.progression_multiplier_ranger = original.progression_multiplier_ranger;
			clone.overlay_alpha_welcome = original.overlay_alpha_welcome;
			clone.progression_multiplier_dagger = original.progression_multiplier_dagger;
			clone.progression_dagger = original.progression_dagger;
			clone.overlay_alpha_dailyquestwarning = original.overlay_alpha_dailyquestwarning;
			clone.dkc_unlocked = original.dkc_unlocked;
			clone.unlocked_quests = original.unlocked_quests;
			clone.finished_quests = original.finished_quests;
			clone.highorcmax = original.highorcmax;
			clone.highorcspawned = original.highorcspawned;
			clone.tuskmax = original.tuskmax;
			clone.tuskspawned = original.tuskspawned;
			clone.reward_1 = original.reward_1;
			clone.reward_2 = original.reward_2;
			clone.reward_3 = original.reward_3;
			clone.dkc_cleared = original.dkc_cleared;
			if (!event.isWasDeath()) {
				clone.LoreAccurateRankStart = original.LoreAccurateRankStart;
				clone.ariset = original.ariset;
				clone.berserk = original.berserk;
				clone.daggermelee = original.daggermelee;
				clone.daggermeleetimer = original.daggermeleetimer;
				clone.dash = original.dash;
				clone.domainef = original.domainef;
				clone.DunRank = original.DunRank;
				clone.Fatigue = original.Fatigue;
				clone.firecharge = original.firecharge;
				clone.firestr = original.firestr;
				clone.FireVar = original.FireVar;
				clone.FRing = original.FRing;
				clone.frostcharge = original.frostcharge;
				clone.FX = original.FX;
				clone.FY = original.FY;
				clone.FZ = original.FZ;
				clone.GolemRage = original.GolemRage;
				clone.guard = original.guard;
				clone.guarding = original.guarding;
				clone.Imbuement = original.Imbuement;
				clone.impct1 = original.impct1;
				clone.instancecomplete = original.instancecomplete;
				clone.inv = original.inv;
				clone.istraining = original.istraining;
				clone.jobadvpoint = original.jobadvpoint;
				clone.JobChange_timer = original.JobChange_timer;
				clone.jobtimer = original.jobtimer;
				clone.JP = original.JP;
				clone.kamishcharge = original.kamishcharge;
				clone.leapjump = original.leapjump;
				clone.leftpunch = original.leftpunch;
				clone.Mana = original.Mana;
				clone.MP = original.MP;
				clone.paralyzenot = original.paralyzenot;
				clone.PhantomName = original.PhantomName;
				clone.punishment = original.punishment;
				clone.questinfo = original.questinfo;
				clone.QuestProgression = original.QuestProgression;
				clone.radius1 = original.radius1;
				clone.rushattack = original.rushattack;
				clone.shieldbash = original.shieldbash;
				clone.Skillcycle = original.Skillcycle;
				clone.slashfur = original.slashfur;
				clone.slashfurrybroad = original.slashfurrybroad;
				clone.slashfurtimer = original.slashfurtimer;
				clone.spiderstat = original.spiderstat;
				clone.tpd = original.tpd;
				clone.TX = original.TX;
				clone.TY = original.TY;
				clone.TZ = original.TZ;
				clone.upforceslash = original.upforceslash;
				clone.wp = original.wp;
				clone.rangerleapnum = original.rangerleapnum;
				clone.rangerleaptimer = original.rangerleaptimer;
				clone.sl_EVA = original.sl_EVA;
				clone.randplayerx = original.randplayerx;
				clone.randplayery = original.randplayery;
				clone.randplayerz = original.randplayerz;
				clone.traintype = original.traintype;
				clone.isdailytraining = original.isdailytraining;
				clone.instance_query_timer = original.instance_query_timer;
				clone.monarchbeam = original.monarchbeam;
				clone.baranlightningstrike = original.baranlightningstrike;
				clone.PslotSelecting = original.PslotSelecting;
				clone.FireRingTimer = original.FireRingTimer;
			}
			if (!event.getEntity().level().isClientSide()) {
				for (Entity entityiterator : new ArrayList<>(event.getEntity().level().players())) {
					((PlayerVariables) entityiterator.getCapability(PLAYER_VARIABLES_CAPABILITY, null).orElse(new PlayerVariables())).syncPlayerVariables(entityiterator);
				}
			}
		}

		@SubscribeEvent
		public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
			if (!event.getEntity().level().isClientSide()) {
				SavedData mapdata = MapVariables.get(event.getEntity().level());
				SavedData worlddata = WorldVariables.get(event.getEntity().level());
				if (mapdata != null)
					SololevelingMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) event.getEntity()), new SavedDataSyncMessage(0, mapdata));
				if (worlddata != null)
					SololevelingMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) event.getEntity()), new SavedDataSyncMessage(1, worlddata));
			}
		}

		@SubscribeEvent
		public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
			if (!event.getEntity().level().isClientSide()) {
				SavedData worlddata = WorldVariables.get(event.getEntity().level());
				if (worlddata != null)
					SololevelingMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) event.getEntity()), new SavedDataSyncMessage(1, worlddata));
			}
		}
	}

	public static class WorldVariables extends SavedData {
		public static final String DATA_NAME = "sololeveling_worldvars";

		public static WorldVariables load(CompoundTag tag) {
			WorldVariables data = new WorldVariables();
			data.read(tag);
			return data;
		}

		public void read(CompoundTag nbt) {
		}

		@Override
		public CompoundTag save(CompoundTag nbt) {
			return nbt;
		}

		public void syncData(LevelAccessor world) {
			this.setDirty();
			if (world instanceof Level level && !level.isClientSide())
				SololevelingMod.PACKET_HANDLER.send(PacketDistributor.DIMENSION.with(level::dimension), new SavedDataSyncMessage(1, this));
		}

		static WorldVariables clientSide = new WorldVariables();

		public static WorldVariables get(LevelAccessor world) {
			if (world instanceof ServerLevel level) {
				return level.getDataStorage().computeIfAbsent(e -> WorldVariables.load(e), WorldVariables::new, DATA_NAME);
			} else {
				return clientSide;
			}
		}
	}

	public static class MapVariables extends SavedData {
		public static final String DATA_NAME = "sololeveling_mapvars";
		public boolean RedGate = false;
		public double gatetimer = 0;
		public double shmlimit = 0;
		public boolean portalreset = false;
		public String GatesCleared = "";
		public String ActiveGateInstances = "\"\"";

		public static MapVariables load(CompoundTag tag) {
			MapVariables data = new MapVariables();
			data.read(tag);
			return data;
		}

		public void read(CompoundTag nbt) {
			if (nbt == null) {
				nbt = save(new CompoundTag());
			}
			RedGate = nbt.getBoolean("RedGate");
			gatetimer = nbt.getDouble("gatetimer");
			shmlimit = nbt.getDouble("shmlimit");
			portalreset = nbt.getBoolean("portalreset");
			GatesCleared = nbt.getString("GatesCleared");
			ActiveGateInstances = nbt.getString("ActiveGateInstances");
		}

		@Override
		public CompoundTag save(CompoundTag nbt) {
			nbt.putBoolean("RedGate", RedGate);
			nbt.putDouble("gatetimer", gatetimer);
			nbt.putDouble("shmlimit", shmlimit);
			nbt.putBoolean("portalreset", portalreset);
			nbt.putString("GatesCleared", GatesCleared);
			nbt.putString("ActiveGateInstances", ActiveGateInstances);
			return nbt;
		}

		public void syncData(LevelAccessor world) {
			this.setDirty();
			if (world instanceof Level && !world.isClientSide())
				SololevelingMod.PACKET_HANDLER.send(PacketDistributor.ALL.noArg(), new SavedDataSyncMessage(0, this));
		}

		static MapVariables clientSide = new MapVariables();

		public static MapVariables get(LevelAccessor world) {
			if (world instanceof ServerLevelAccessor serverLevelAcc) {
				return serverLevelAcc.getLevel().getServer().getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(e -> MapVariables.load(e), MapVariables::new, DATA_NAME);
			} else {
				return clientSide;
			}
		}
	}

	public static class SavedDataSyncMessage {
		private final int type;
		private SavedData data;

		public SavedDataSyncMessage(FriendlyByteBuf buffer) {
			this.type = buffer.readInt();
			CompoundTag nbt = buffer.readNbt();
			if (nbt != null) {
				this.data = this.type == 0 ? new MapVariables() : new WorldVariables();
				if (this.data instanceof MapVariables mapVariables)
					mapVariables.read(nbt);
				else if (this.data instanceof WorldVariables worldVariables)
					worldVariables.read(nbt);
			}
		}

		public SavedDataSyncMessage(int type, SavedData data) {
			this.type = type;
			this.data = data;
		}

		public static void buffer(SavedDataSyncMessage message, FriendlyByteBuf buffer) {
			buffer.writeInt(message.type);
			if (message.data != null)
				buffer.writeNbt(message.data.save(new CompoundTag()));
		}

		public static void handler(SavedDataSyncMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
			NetworkEvent.Context context = contextSupplier.get();
			context.enqueueWork(() -> {
				if (!context.getDirection().getReceptionSide().isServer() && message.data != null) {
					if (message.type == 0)
						MapVariables.clientSide = (MapVariables) message.data;
					else
						WorldVariables.clientSide = (WorldVariables) message.data;
				}
			});
			context.setPacketHandled(true);
		}
	}

	public static final Capability<PlayerVariables> PLAYER_VARIABLES_CAPABILITY = CapabilityManager.get(new CapabilityToken<PlayerVariables>() {
	});

	@Mod.EventBusSubscriber
	private static class PlayerVariablesProvider implements ICapabilitySerializable<Tag> {
		@SubscribeEvent
		public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
			if (event.getObject() instanceof Player && !(event.getObject() instanceof FakePlayer))
				event.addCapability(new ResourceLocation("sololeveling", "player_variables"), new PlayerVariablesProvider());
		}

		private final PlayerVariables playerVariables = new PlayerVariables();
		private final LazyOptional<PlayerVariables> instance = LazyOptional.of(() -> playerVariables);

		@Override
		public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
			return cap == PLAYER_VARIABLES_CAPABILITY ? instance.cast() : LazyOptional.empty();
		}

		@Override
		public Tag serializeNBT() {
			return playerVariables.writeNBT();
		}

		@Override
		public void deserializeNBT(Tag nbt) {
			playerVariables.readNBT(nbt);
		}
	}

	public static class PlayerVariables {
		public ItemStack shopitem5 = ItemStack.EMPTY;
		public double LoreAccurateRankStart = 1.0;
		public double ariset = 0;
		public boolean berserk = false;
		public boolean daggermelee = false;
		public double daggermeleetimer = 0;
		public double dash = 1.0;
		public boolean domainef = false;
		public double DunRank = 0;
		public double Fatigue = 0;
		public double firecharge = 0;
		public double firestr = 0;
		public double FireVar = 0;
		public boolean FRing = false;
		public double frostcharge = 0;
		public double FX = 0;
		public double FY = 0;
		public double FZ = 0;
		public boolean GolemRage = false;
		public boolean guard = false;
		public double guarding = 50.0;
		public boolean Imbuement = false;
		public double impct1 = 0;
		public boolean instancecomplete = false;
		public boolean inv = false;
		public boolean istraining = false;
		public double jobadvpoint = 0;
		public double JobChange_timer = 0;
		public double jobtimer = 0;
		public boolean JP = false;
		public double kamishcharge = 0;
		public boolean leapjump = false;
		public boolean leftpunch = false;
		public double Mana = 0;
		public double MP = 0;
		public boolean paralyzenot = false;
		public String PhantomName = "\"\"";
		public double punishment = 0;
		public boolean questinfo = false;
		public double QuestProgression = 0;
		public double radius1 = 0;
		public boolean rushattack = false;
		public boolean shieldbash = false;
		public double Skillcycle = 0.0;
		public boolean slashfur = false;
		public boolean slashfurrybroad = false;
		public double slashfurtimer = 0;
		public boolean spiderstat = false;
		public boolean tpd = false;
		public double TX = 0;
		public double TY = 0;
		public double TZ = 0;
		public boolean upforceslash = false;
		public double wp = 0;
		public double rangerleapnum = 0;
		public double rangerleaptimer = 0;
		public double sl_EVA = 0;
		public double randplayerx = 0;
		public double randplayery = 0;
		public double randplayerz = 0;
		public String traintype = "";
		public boolean isdailytraining = false;
		public ItemStack shopitem1 = ItemStack.EMPTY;
		public ItemStack shopitem2 = ItemStack.EMPTY;
		public ItemStack shopitem3 = ItemStack.EMPTY;
		public ItemStack shopitem4 = ItemStack.EMPTY;
		public ItemStack shopitem6 = ItemStack.EMPTY;
		public boolean Ab1 = true;
		public boolean Ab2 = true;
		public boolean Ab3 = true;
		public boolean Ab4 = true;
		public String abilities = "\"\"";
		public boolean ActiveDaily = false;
		public boolean alivestatus = false;
		public double beru = 0;
		public double berumax = 0;
		public double boss = 0;
		public boolean BossKilled = false;
		public boolean Call4Death = false;
		public double Classes = 0;
		public boolean combatmode = false;
		public boolean commanddeath = false;
		public double dailykilltyppe = 0;
		public double dailysecrettrans = 1.0;
		public double dailytasks = 0;
		public double dailytimer = 0.0;
		public double DeathX = 0;
		public double DeathY = 0;
		public double DeathZ = 0;
		public String Dialogue = "";
		public double domain = 0;
		public boolean dungeoning = false;
		public double DungeonNum = 0;
		public double DunX = 0;
		public double DunY = 0;
		public double DunZ = 0;
		public double Durability = 0;
		public boolean giftstatus = false;
		public double GobShadow = 0;
		public double GobShadowMax = 0;
		public double golds = 0;
		public double guardbar = 0;
		public double GuildCode = 0;
		public boolean HunterEyes = false;
		public double HunterRank = 0;
		public double igris = 0;
		public double IgrisSpawned = 0;
		public double Intelligence = 0;
		public double investvalue = 1.0;
		public double JOB = 0.0;
		public boolean jobkey = false;
		public double killmission = 9.0;
		public double LastKilled = 0;
		public double Level = 0;
		public String MainQuest = "";
		public double manaregen = 0;
		public double MaxXP = 10.0;
		public double orcmax = 0;
		public double orcspawned = 0;
		public double OrdShadow = 0;
		public double ordshadowmax = 0;
		public ItemStack overridefeet = ItemStack.EMPTY;
		public ItemStack overridehead = ItemStack.EMPTY;
		public ItemStack overridelegs = ItemStack.EMPTY;
		public ItemStack overridetorso = ItemStack.EMPTY;
		public double perception = 0;
		public boolean Player = true;
		public double polarbear = 0;
		public double polarbearmax = 0;
		public double pushup = 0;
		public String ranking = "";
		public double rankingnum = 0;
		public boolean resistance = false;
		public double RUN = 0;
		public double RX = 0;
		public double RZ = 0;
		public boolean ShadowExchange = false;
		public double ShadowSelect = 1.0;
		public double shadowstorage = 10.0;
		public double shadowstorageusage = 0;
		public double situp = 0;
		public double SkillPoints = 0;
		public double slashfury = 0;
		public double Speed = 0;
		public double speedpercent = 100.0;
		public double squat = 0;
		public double statshown = 0;
		public double Strength = 0;
		public double summonlimit = 0.0;
		public double summonlimitusage = 0;
		public double tj = 0;
		public boolean tjonoff = true;
		public double Vitality = 0;
		public double WolfShadow = 0;
		public double WolfShadowMax = 0;
		public double Xp = 0;
		public double xpmultiplier = 1.0;
		public double Money = 0;
		public boolean CustomHUD = true;
		public double ShadowGoblinArcherAmount = 0;
		public double ShadowGoblinMageAmount = 0;
		public double ShadowGoblinArcherMax = 0;
		public double ShadowGoblinMageMax = 0;
		public double shadowdragonnum = 0;
		public double shadowdragonmax = 0;
		public double packetCounter = 0;
		public double instance_query_timer = 0;
		public double daily_refreshes = 0;
		public boolean selection = false;
		public String party = "";
		public boolean monarchbeam = false;
		public double baranlightningstrike = 0;
		public double prevRank = 0;
		public double prevLevel = 0;
		public double idcd = 0;
		public double title = 0;
		public String Plist = ".";
		public String Pslot1 = "";
		public String Pslot2 = "";
		public String Pslot3 = "";
		public String Pslot4 = "";
		public String Pslot5 = "";
		public String Pslot6 = "";
		public String Pslot7 = "";
		public String Pslot8 = "";
		public double PslotSelecting = 0;
		public String PselectedPower = "";
		public double progression_assassin = 0;
		public double progression_mage = 0;
		public double progression_fighter = 0;
		public double progression_tanker = 0;
		public double progression_healer = 0;
		public double progression_ranger = 0;
		public String JobSkills = "\"\"";
		public String ExchangeDimensions = ".";
		public String ExchangeCords = ".";
		public boolean ShadowBody = false;
		public double progression_multiplier_assassin = 1.0;
		public double progression_multiplier_mage = 1.0;
		public double progression_multiplier_fighter = 1.0;
		public double progression_multiplier_tanker = 1.0;
		public double progression_multiplier_healer = 1.0;
		public double progression_multiplier_ranger = 1.0;
		public double overlay_alpha_welcome = 0;
		public double progression_multiplier_dagger = 1.0;
		public double progression_dagger = 0;
		public double overlay_alpha_dailyquestwarning = 0;
		public double dkc_unlocked = 0;
		public String unlocked_quests = "\"\"";
		public String finished_quests = "\"\"";
		public double highorcmax = 0;
		public double highorcspawned = 0;
		public double tuskmax = 0;
		public double tuskspawned = 0;
		public double FireRingTimer = 0;
		public String reward_1 = "\"\"";
		public String reward_2 = "\"\"";
		public String reward_3 = "\"\"";
		public double dkc_cleared = 0;

		public void syncPlayerVariables(Entity entity) {
			if (entity instanceof ServerPlayer serverPlayer)
				SololevelingMod.PACKET_HANDLER.send(PacketDistributor.DIMENSION.with(entity.level()::dimension), new PlayerVariablesSyncMessage(this, entity.getId()));
		}

		public Tag writeNBT() {
			CompoundTag nbt = new CompoundTag();
			nbt.put("shopitem5", shopitem5.save(new CompoundTag()));
			nbt.putDouble("LoreAccurateRankStart", LoreAccurateRankStart);
			nbt.putDouble("ariset", ariset);
			nbt.putBoolean("berserk", berserk);
			nbt.putBoolean("daggermelee", daggermelee);
			nbt.putDouble("daggermeleetimer", daggermeleetimer);
			nbt.putDouble("dash", dash);
			nbt.putBoolean("domainef", domainef);
			nbt.putDouble("DunRank", DunRank);
			nbt.putDouble("Fatigue", Fatigue);
			nbt.putDouble("firecharge", firecharge);
			nbt.putDouble("firestr", firestr);
			nbt.putDouble("FireVar", FireVar);
			nbt.putBoolean("FRing", FRing);
			nbt.putDouble("frostcharge", frostcharge);
			nbt.putDouble("FX", FX);
			nbt.putDouble("FY", FY);
			nbt.putDouble("FZ", FZ);
			nbt.putBoolean("GolemRage", GolemRage);
			nbt.putBoolean("guard", guard);
			nbt.putDouble("guarding", guarding);
			nbt.putBoolean("Imbuement", Imbuement);
			nbt.putDouble("impct1", impct1);
			nbt.putBoolean("instancecomplete", instancecomplete);
			nbt.putBoolean("inv", inv);
			nbt.putBoolean("istraining", istraining);
			nbt.putDouble("jobadvpoint", jobadvpoint);
			nbt.putDouble("JobChange_timer", JobChange_timer);
			nbt.putDouble("jobtimer", jobtimer);
			nbt.putBoolean("JP", JP);
			nbt.putDouble("kamishcharge", kamishcharge);
			nbt.putBoolean("leapjump", leapjump);
			nbt.putBoolean("leftpunch", leftpunch);
			nbt.putDouble("Mana", Mana);
			nbt.putDouble("MP", MP);
			nbt.putBoolean("paralyzenot", paralyzenot);
			nbt.putString("PhantomName", PhantomName);
			nbt.putDouble("punishment", punishment);
			nbt.putBoolean("questinfo", questinfo);
			nbt.putDouble("QuestProgression", QuestProgression);
			nbt.putDouble("radius1", radius1);
			nbt.putBoolean("rushattack", rushattack);
			nbt.putBoolean("shieldbash", shieldbash);
			nbt.putDouble("Skillcycle", Skillcycle);
			nbt.putBoolean("slashfur", slashfur);
			nbt.putBoolean("slashfurrybroad", slashfurrybroad);
			nbt.putDouble("slashfurtimer", slashfurtimer);
			nbt.putBoolean("spiderstat", spiderstat);
			nbt.putBoolean("tpd", tpd);
			nbt.putDouble("TX", TX);
			nbt.putDouble("TY", TY);
			nbt.putDouble("TZ", TZ);
			nbt.putBoolean("upforceslash", upforceslash);
			nbt.putDouble("wp", wp);
			nbt.putDouble("rangerleapnum", rangerleapnum);
			nbt.putDouble("rangerleaptimer", rangerleaptimer);
			nbt.putDouble("sl_EVA", sl_EVA);
			nbt.putDouble("randplayerx", randplayerx);
			nbt.putDouble("randplayery", randplayery);
			nbt.putDouble("randplayerz", randplayerz);
			nbt.putString("traintype", traintype);
			nbt.putBoolean("isdailytraining", isdailytraining);
			nbt.put("shopitem1", shopitem1.save(new CompoundTag()));
			nbt.put("shopitem2", shopitem2.save(new CompoundTag()));
			nbt.put("shopitem3", shopitem3.save(new CompoundTag()));
			nbt.put("shopitem4", shopitem4.save(new CompoundTag()));
			nbt.put("shopitem6", shopitem6.save(new CompoundTag()));
			nbt.putBoolean("Ab1", Ab1);
			nbt.putBoolean("Ab2", Ab2);
			nbt.putBoolean("Ab3", Ab3);
			nbt.putBoolean("Ab4", Ab4);
			nbt.putString("abilities", abilities);
			nbt.putBoolean("ActiveDaily", ActiveDaily);
			nbt.putBoolean("alivestatus", alivestatus);
			nbt.putDouble("beru", beru);
			nbt.putDouble("berumax", berumax);
			nbt.putDouble("boss", boss);
			nbt.putBoolean("BossKilled", BossKilled);
			nbt.putBoolean("Call4Death", Call4Death);
			nbt.putDouble("Classes", Classes);
			nbt.putBoolean("combatmode", combatmode);
			nbt.putBoolean("commanddeath", commanddeath);
			nbt.putDouble("dailykilltyppe", dailykilltyppe);
			nbt.putDouble("dailysecrettrans", dailysecrettrans);
			nbt.putDouble("dailytasks", dailytasks);
			nbt.putDouble("dailytimer", dailytimer);
			nbt.putDouble("DeathX", DeathX);
			nbt.putDouble("DeathY", DeathY);
			nbt.putDouble("DeathZ", DeathZ);
			nbt.putString("Dialogue", Dialogue);
			nbt.putDouble("domain", domain);
			nbt.putBoolean("dungeoning", dungeoning);
			nbt.putDouble("DungeonNum", DungeonNum);
			nbt.putDouble("DunX", DunX);
			nbt.putDouble("DunY", DunY);
			nbt.putDouble("DunZ", DunZ);
			nbt.putDouble("Durability", Durability);
			nbt.putBoolean("giftstatus", giftstatus);
			nbt.putDouble("GobShadow", GobShadow);
			nbt.putDouble("GobShadowMax", GobShadowMax);
			nbt.putDouble("golds", golds);
			nbt.putDouble("guardbar", guardbar);
			nbt.putDouble("GuildCode", GuildCode);
			nbt.putBoolean("HunterEyes", HunterEyes);
			nbt.putDouble("HunterRank", HunterRank);
			nbt.putDouble("igris", igris);
			nbt.putDouble("IgrisSpawned", IgrisSpawned);
			nbt.putDouble("Intelligence", Intelligence);
			nbt.putDouble("investvalue", investvalue);
			nbt.putDouble("JOB", JOB);
			nbt.putBoolean("jobkey", jobkey);
			nbt.putDouble("killmission", killmission);
			nbt.putDouble("LastKilled", LastKilled);
			nbt.putDouble("Level", Level);
			nbt.putString("MainQuest", MainQuest);
			nbt.putDouble("manaregen", manaregen);
			nbt.putDouble("MaxXP", MaxXP);
			nbt.putDouble("orcmax", orcmax);
			nbt.putDouble("orcspawned", orcspawned);
			nbt.putDouble("OrdShadow", OrdShadow);
			nbt.putDouble("ordshadowmax", ordshadowmax);
			nbt.put("overridefeet", overridefeet.save(new CompoundTag()));
			nbt.put("overridehead", overridehead.save(new CompoundTag()));
			nbt.put("overridelegs", overridelegs.save(new CompoundTag()));
			nbt.put("overridetorso", overridetorso.save(new CompoundTag()));
			nbt.putDouble("perception", perception);
			nbt.putBoolean("Player", Player);
			nbt.putDouble("polarbear", polarbear);
			nbt.putDouble("polarbearmax", polarbearmax);
			nbt.putDouble("pushup", pushup);
			nbt.putString("ranking", ranking);
			nbt.putDouble("rankingnum", rankingnum);
			nbt.putBoolean("resistance", resistance);
			nbt.putDouble("RUN", RUN);
			nbt.putDouble("RX", RX);
			nbt.putDouble("RZ", RZ);
			nbt.putBoolean("ShadowExchange", ShadowExchange);
			nbt.putDouble("ShadowSelect", ShadowSelect);
			nbt.putDouble("shadowstorage", shadowstorage);
			nbt.putDouble("shadowstorageusage", shadowstorageusage);
			nbt.putDouble("situp", situp);
			nbt.putDouble("SkillPoints", SkillPoints);
			nbt.putDouble("slashfury", slashfury);
			nbt.putDouble("Speed", Speed);
			nbt.putDouble("speedpercent", speedpercent);
			nbt.putDouble("squat", squat);
			nbt.putDouble("statshown", statshown);
			nbt.putDouble("Strength", Strength);
			nbt.putDouble("summonlimit", summonlimit);
			nbt.putDouble("summonlimitusage", summonlimitusage);
			nbt.putDouble("tj", tj);
			nbt.putBoolean("tjonoff", tjonoff);
			nbt.putDouble("Vitality", Vitality);
			nbt.putDouble("WolfShadow", WolfShadow);
			nbt.putDouble("WolfShadowMax", WolfShadowMax);
			nbt.putDouble("Xp", Xp);
			nbt.putDouble("xpmultiplier", xpmultiplier);
			nbt.putDouble("Money", Money);
			nbt.putBoolean("CustomHUD", CustomHUD);
			nbt.putDouble("ShadowGoblinArcherAmount", ShadowGoblinArcherAmount);
			nbt.putDouble("ShadowGoblinMageAmount", ShadowGoblinMageAmount);
			nbt.putDouble("ShadowGoblinArcherMax", ShadowGoblinArcherMax);
			nbt.putDouble("ShadowGoblinMageMax", ShadowGoblinMageMax);
			nbt.putDouble("shadowdragonnum", shadowdragonnum);
			nbt.putDouble("shadowdragonmax", shadowdragonmax);
			nbt.putDouble("packetCounter", packetCounter);
			nbt.putDouble("instance_query_timer", instance_query_timer);
			nbt.putDouble("daily_refreshes", daily_refreshes);
			nbt.putBoolean("selection", selection);
			nbt.putString("party", party);
			nbt.putBoolean("monarchbeam", monarchbeam);
			nbt.putDouble("baranlightningstrike", baranlightningstrike);
			nbt.putDouble("prevRank", prevRank);
			nbt.putDouble("prevLevel", prevLevel);
			nbt.putDouble("idcd", idcd);
			nbt.putDouble("title", title);
			nbt.putString("Plist", Plist);
			nbt.putString("Pslot1", Pslot1);
			nbt.putString("Pslot2", Pslot2);
			nbt.putString("Pslot3", Pslot3);
			nbt.putString("Pslot4", Pslot4);
			nbt.putString("Pslot5", Pslot5);
			nbt.putString("Pslot6", Pslot6);
			nbt.putString("Pslot7", Pslot7);
			nbt.putString("Pslot8", Pslot8);
			nbt.putDouble("PslotSelecting", PslotSelecting);
			nbt.putString("PselectedPower", PselectedPower);
			nbt.putDouble("progression_assassin", progression_assassin);
			nbt.putDouble("progression_mage", progression_mage);
			nbt.putDouble("progression_fighter", progression_fighter);
			nbt.putDouble("progression_tanker", progression_tanker);
			nbt.putDouble("progression_healer", progression_healer);
			nbt.putDouble("progression_ranger", progression_ranger);
			nbt.putString("JobSkills", JobSkills);
			nbt.putString("ExchangeDimensions", ExchangeDimensions);
			nbt.putString("ExchangeCords", ExchangeCords);
			nbt.putBoolean("ShadowBody", ShadowBody);
			nbt.putDouble("progression_multiplier_assassin", progression_multiplier_assassin);
			nbt.putDouble("progression_multiplier_mage", progression_multiplier_mage);
			nbt.putDouble("progression_multiplier_fighter", progression_multiplier_fighter);
			nbt.putDouble("progression_multiplier_tanker", progression_multiplier_tanker);
			nbt.putDouble("progression_multiplier_healer", progression_multiplier_healer);
			nbt.putDouble("progression_multiplier_ranger", progression_multiplier_ranger);
			nbt.putDouble("overlay_alpha_welcome", overlay_alpha_welcome);
			nbt.putDouble("progression_multiplier_dagger", progression_multiplier_dagger);
			nbt.putDouble("progression_dagger", progression_dagger);
			nbt.putDouble("overlay_alpha_dailyquestwarning", overlay_alpha_dailyquestwarning);
			nbt.putDouble("dkc_unlocked", dkc_unlocked);
			nbt.putString("unlocked_quests", unlocked_quests);
			nbt.putString("finished_quests", finished_quests);
			nbt.putDouble("highorcmax", highorcmax);
			nbt.putDouble("highorcspawned", highorcspawned);
			nbt.putDouble("tuskmax", tuskmax);
			nbt.putDouble("tuskspawned", tuskspawned);
			nbt.putDouble("FireRingTimer", FireRingTimer);
			nbt.putString("reward_1", reward_1);
			nbt.putString("reward_2", reward_2);
			nbt.putString("reward_3", reward_3);
			nbt.putDouble("dkc_cleared", dkc_cleared);
			return nbt;
		}

		public void readNBT(Tag tag) {
			if (tag == null) {
				tag = writeNBT();
			}
			CompoundTag nbt = (CompoundTag) tag;
			if (nbt == null) {
				nbt = (CompoundTag) writeNBT();
			}
			shopitem5 = ItemStack.of(nbt.getCompound("shopitem5"));
			LoreAccurateRankStart = nbt.getDouble("LoreAccurateRankStart");
			ariset = nbt.getDouble("ariset");
			berserk = nbt.getBoolean("berserk");
			daggermelee = nbt.getBoolean("daggermelee");
			daggermeleetimer = nbt.getDouble("daggermeleetimer");
			dash = nbt.getDouble("dash");
			domainef = nbt.getBoolean("domainef");
			DunRank = nbt.getDouble("DunRank");
			Fatigue = nbt.getDouble("Fatigue");
			firecharge = nbt.getDouble("firecharge");
			firestr = nbt.getDouble("firestr");
			FireVar = nbt.getDouble("FireVar");
			FRing = nbt.getBoolean("FRing");
			frostcharge = nbt.getDouble("frostcharge");
			FX = nbt.getDouble("FX");
			FY = nbt.getDouble("FY");
			FZ = nbt.getDouble("FZ");
			GolemRage = nbt.getBoolean("GolemRage");
			guard = nbt.getBoolean("guard");
			guarding = nbt.getDouble("guarding");
			Imbuement = nbt.getBoolean("Imbuement");
			impct1 = nbt.getDouble("impct1");
			instancecomplete = nbt.getBoolean("instancecomplete");
			inv = nbt.getBoolean("inv");
			istraining = nbt.getBoolean("istraining");
			jobadvpoint = nbt.getDouble("jobadvpoint");
			JobChange_timer = nbt.getDouble("JobChange_timer");
			jobtimer = nbt.getDouble("jobtimer");
			JP = nbt.getBoolean("JP");
			kamishcharge = nbt.getDouble("kamishcharge");
			leapjump = nbt.getBoolean("leapjump");
			leftpunch = nbt.getBoolean("leftpunch");
			Mana = nbt.getDouble("Mana");
			MP = nbt.getDouble("MP");
			paralyzenot = nbt.getBoolean("paralyzenot");
			PhantomName = nbt.getString("PhantomName");
			punishment = nbt.getDouble("punishment");
			questinfo = nbt.getBoolean("questinfo");
			QuestProgression = nbt.getDouble("QuestProgression");
			radius1 = nbt.getDouble("radius1");
			rushattack = nbt.getBoolean("rushattack");
			shieldbash = nbt.getBoolean("shieldbash");
			Skillcycle = nbt.getDouble("Skillcycle");
			slashfur = nbt.getBoolean("slashfur");
			slashfurrybroad = nbt.getBoolean("slashfurrybroad");
			slashfurtimer = nbt.getDouble("slashfurtimer");
			spiderstat = nbt.getBoolean("spiderstat");
			tpd = nbt.getBoolean("tpd");
			TX = nbt.getDouble("TX");
			TY = nbt.getDouble("TY");
			TZ = nbt.getDouble("TZ");
			upforceslash = nbt.getBoolean("upforceslash");
			wp = nbt.getDouble("wp");
			rangerleapnum = nbt.getDouble("rangerleapnum");
			rangerleaptimer = nbt.getDouble("rangerleaptimer");
			sl_EVA = nbt.getDouble("sl_EVA");
			randplayerx = nbt.getDouble("randplayerx");
			randplayery = nbt.getDouble("randplayery");
			randplayerz = nbt.getDouble("randplayerz");
			traintype = nbt.getString("traintype");
			isdailytraining = nbt.getBoolean("isdailytraining");
			shopitem1 = ItemStack.of(nbt.getCompound("shopitem1"));
			shopitem2 = ItemStack.of(nbt.getCompound("shopitem2"));
			shopitem3 = ItemStack.of(nbt.getCompound("shopitem3"));
			shopitem4 = ItemStack.of(nbt.getCompound("shopitem4"));
			shopitem6 = ItemStack.of(nbt.getCompound("shopitem6"));
			Ab1 = nbt.getBoolean("Ab1");
			Ab2 = nbt.getBoolean("Ab2");
			Ab3 = nbt.getBoolean("Ab3");
			Ab4 = nbt.getBoolean("Ab4");
			abilities = nbt.getString("abilities");
			ActiveDaily = nbt.getBoolean("ActiveDaily");
			alivestatus = nbt.getBoolean("alivestatus");
			beru = nbt.getDouble("beru");
			berumax = nbt.getDouble("berumax");
			boss = nbt.getDouble("boss");
			BossKilled = nbt.getBoolean("BossKilled");
			Call4Death = nbt.getBoolean("Call4Death");
			Classes = nbt.getDouble("Classes");
			combatmode = nbt.getBoolean("combatmode");
			commanddeath = nbt.getBoolean("commanddeath");
			dailykilltyppe = nbt.getDouble("dailykilltyppe");
			dailysecrettrans = nbt.getDouble("dailysecrettrans");
			dailytasks = nbt.getDouble("dailytasks");
			dailytimer = nbt.getDouble("dailytimer");
			DeathX = nbt.getDouble("DeathX");
			DeathY = nbt.getDouble("DeathY");
			DeathZ = nbt.getDouble("DeathZ");
			Dialogue = nbt.getString("Dialogue");
			domain = nbt.getDouble("domain");
			dungeoning = nbt.getBoolean("dungeoning");
			DungeonNum = nbt.getDouble("DungeonNum");
			DunX = nbt.getDouble("DunX");
			DunY = nbt.getDouble("DunY");
			DunZ = nbt.getDouble("DunZ");
			Durability = nbt.getDouble("Durability");
			giftstatus = nbt.getBoolean("giftstatus");
			GobShadow = nbt.getDouble("GobShadow");
			GobShadowMax = nbt.getDouble("GobShadowMax");
			golds = nbt.getDouble("golds");
			guardbar = nbt.getDouble("guardbar");
			GuildCode = nbt.getDouble("GuildCode");
			HunterEyes = nbt.getBoolean("HunterEyes");
			HunterRank = nbt.getDouble("HunterRank");
			igris = nbt.getDouble("igris");
			IgrisSpawned = nbt.getDouble("IgrisSpawned");
			Intelligence = nbt.getDouble("Intelligence");
			investvalue = nbt.getDouble("investvalue");
			JOB = nbt.getDouble("JOB");
			jobkey = nbt.getBoolean("jobkey");
			killmission = nbt.getDouble("killmission");
			LastKilled = nbt.getDouble("LastKilled");
			Level = nbt.getDouble("Level");
			MainQuest = nbt.getString("MainQuest");
			manaregen = nbt.getDouble("manaregen");
			MaxXP = nbt.getDouble("MaxXP");
			orcmax = nbt.getDouble("orcmax");
			orcspawned = nbt.getDouble("orcspawned");
			OrdShadow = nbt.getDouble("OrdShadow");
			ordshadowmax = nbt.getDouble("ordshadowmax");
			overridefeet = ItemStack.of(nbt.getCompound("overridefeet"));
			overridehead = ItemStack.of(nbt.getCompound("overridehead"));
			overridelegs = ItemStack.of(nbt.getCompound("overridelegs"));
			overridetorso = ItemStack.of(nbt.getCompound("overridetorso"));
			perception = nbt.getDouble("perception");
			Player = nbt.getBoolean("Player");
			polarbear = nbt.getDouble("polarbear");
			polarbearmax = nbt.getDouble("polarbearmax");
			pushup = nbt.getDouble("pushup");
			ranking = nbt.getString("ranking");
			rankingnum = nbt.getDouble("rankingnum");
			resistance = nbt.getBoolean("resistance");
			RUN = nbt.getDouble("RUN");
			RX = nbt.getDouble("RX");
			RZ = nbt.getDouble("RZ");
			ShadowExchange = nbt.getBoolean("ShadowExchange");
			ShadowSelect = nbt.getDouble("ShadowSelect");
			shadowstorage = nbt.getDouble("shadowstorage");
			shadowstorageusage = nbt.getDouble("shadowstorageusage");
			situp = nbt.getDouble("situp");
			SkillPoints = nbt.getDouble("SkillPoints");
			slashfury = nbt.getDouble("slashfury");
			Speed = nbt.getDouble("Speed");
			speedpercent = nbt.getDouble("speedpercent");
			squat = nbt.getDouble("squat");
			statshown = nbt.getDouble("statshown");
			Strength = nbt.getDouble("Strength");
			summonlimit = nbt.getDouble("summonlimit");
			summonlimitusage = nbt.getDouble("summonlimitusage");
			tj = nbt.getDouble("tj");
			tjonoff = nbt.getBoolean("tjonoff");
			Vitality = nbt.getDouble("Vitality");
			WolfShadow = nbt.getDouble("WolfShadow");
			WolfShadowMax = nbt.getDouble("WolfShadowMax");
			Xp = nbt.getDouble("Xp");
			xpmultiplier = nbt.getDouble("xpmultiplier");
			Money = nbt.getDouble("Money");
			CustomHUD = nbt.getBoolean("CustomHUD");
			ShadowGoblinArcherAmount = nbt.getDouble("ShadowGoblinArcherAmount");
			ShadowGoblinMageAmount = nbt.getDouble("ShadowGoblinMageAmount");
			ShadowGoblinArcherMax = nbt.getDouble("ShadowGoblinArcherMax");
			ShadowGoblinMageMax = nbt.getDouble("ShadowGoblinMageMax");
			shadowdragonnum = nbt.getDouble("shadowdragonnum");
			shadowdragonmax = nbt.getDouble("shadowdragonmax");
			packetCounter = nbt.getDouble("packetCounter");
			instance_query_timer = nbt.getDouble("instance_query_timer");
			daily_refreshes = nbt.getDouble("daily_refreshes");
			selection = nbt.getBoolean("selection");
			party = nbt.getString("party");
			monarchbeam = nbt.getBoolean("monarchbeam");
			baranlightningstrike = nbt.getDouble("baranlightningstrike");
			prevRank = nbt.getDouble("prevRank");
			prevLevel = nbt.getDouble("prevLevel");
			idcd = nbt.getDouble("idcd");
			title = nbt.getDouble("title");
			Plist = nbt.getString("Plist");
			Pslot1 = nbt.getString("Pslot1");
			Pslot2 = nbt.getString("Pslot2");
			Pslot3 = nbt.getString("Pslot3");
			Pslot4 = nbt.getString("Pslot4");
			Pslot5 = nbt.getString("Pslot5");
			Pslot6 = nbt.getString("Pslot6");
			Pslot7 = nbt.getString("Pslot7");
			Pslot8 = nbt.getString("Pslot8");
			PslotSelecting = nbt.getDouble("PslotSelecting");
			PselectedPower = nbt.getString("PselectedPower");
			progression_assassin = nbt.getDouble("progression_assassin");
			progression_mage = nbt.getDouble("progression_mage");
			progression_fighter = nbt.getDouble("progression_fighter");
			progression_tanker = nbt.getDouble("progression_tanker");
			progression_healer = nbt.getDouble("progression_healer");
			progression_ranger = nbt.getDouble("progression_ranger");
			JobSkills = nbt.getString("JobSkills");
			ExchangeDimensions = nbt.getString("ExchangeDimensions");
			ExchangeCords = nbt.getString("ExchangeCords");
			ShadowBody = nbt.getBoolean("ShadowBody");
			progression_multiplier_assassin = nbt.getDouble("progression_multiplier_assassin");
			progression_multiplier_mage = nbt.getDouble("progression_multiplier_mage");
			progression_multiplier_fighter = nbt.getDouble("progression_multiplier_fighter");
			progression_multiplier_tanker = nbt.getDouble("progression_multiplier_tanker");
			progression_multiplier_healer = nbt.getDouble("progression_multiplier_healer");
			progression_multiplier_ranger = nbt.getDouble("progression_multiplier_ranger");
			overlay_alpha_welcome = nbt.getDouble("overlay_alpha_welcome");
			progression_multiplier_dagger = nbt.getDouble("progression_multiplier_dagger");
			progression_dagger = nbt.getDouble("progression_dagger");
			overlay_alpha_dailyquestwarning = nbt.getDouble("overlay_alpha_dailyquestwarning");
			dkc_unlocked = nbt.getDouble("dkc_unlocked");
			unlocked_quests = nbt.getString("unlocked_quests");
			finished_quests = nbt.getString("finished_quests");
			highorcmax = nbt.getDouble("highorcmax");
			highorcspawned = nbt.getDouble("highorcspawned");
			tuskmax = nbt.getDouble("tuskmax");
			tuskspawned = nbt.getDouble("tuskspawned");
			FireRingTimer = nbt.getDouble("FireRingTimer");
			reward_1 = nbt.getString("reward_1");
			reward_2 = nbt.getString("reward_2");
			reward_3 = nbt.getString("reward_3");
			dkc_cleared = nbt.getDouble("dkc_cleared");
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		SololevelingMod.addNetworkMessage(PlayerVariablesSyncMessage.class, PlayerVariablesSyncMessage::buffer, PlayerVariablesSyncMessage::new, PlayerVariablesSyncMessage::handler);
	}

	public static class PlayerVariablesSyncMessage {
		private final int target;
		private final PlayerVariables data;

		public PlayerVariablesSyncMessage(FriendlyByteBuf buffer) {
			this.data = new PlayerVariables();
			this.data.readNBT(buffer.readNbt());
			this.target = buffer.readInt();
		}

		public PlayerVariablesSyncMessage(PlayerVariables data, int entityid) {
			this.data = data;
			this.target = entityid;
		}

		public static void buffer(PlayerVariablesSyncMessage message, FriendlyByteBuf buffer) {
			buffer.writeNbt((CompoundTag) message.data.writeNBT());
			buffer.writeInt(message.target);
		}

		public static void handler(PlayerVariablesSyncMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
			NetworkEvent.Context context = contextSupplier.get();
			context.enqueueWork(() -> {
				if (!context.getDirection().getReceptionSide().isServer()) {
					PlayerVariables variables = ((PlayerVariables) Minecraft.getInstance().player.level().getEntity(message.target).getCapability(PLAYER_VARIABLES_CAPABILITY, null).orElse(new PlayerVariables()));
					variables.shopitem5 = message.data.shopitem5;
					variables.LoreAccurateRankStart = message.data.LoreAccurateRankStart;
					variables.ariset = message.data.ariset;
					variables.berserk = message.data.berserk;
					variables.daggermelee = message.data.daggermelee;
					variables.daggermeleetimer = message.data.daggermeleetimer;
					variables.dash = message.data.dash;
					variables.domainef = message.data.domainef;
					variables.DunRank = message.data.DunRank;
					variables.Fatigue = message.data.Fatigue;
					variables.firecharge = message.data.firecharge;
					variables.firestr = message.data.firestr;
					variables.FireVar = message.data.FireVar;
					variables.FRing = message.data.FRing;
					variables.frostcharge = message.data.frostcharge;
					variables.FX = message.data.FX;
					variables.FY = message.data.FY;
					variables.FZ = message.data.FZ;
					variables.GolemRage = message.data.GolemRage;
					variables.guard = message.data.guard;
					variables.guarding = message.data.guarding;
					variables.Imbuement = message.data.Imbuement;
					variables.impct1 = message.data.impct1;
					variables.instancecomplete = message.data.instancecomplete;
					variables.inv = message.data.inv;
					variables.istraining = message.data.istraining;
					variables.jobadvpoint = message.data.jobadvpoint;
					variables.JobChange_timer = message.data.JobChange_timer;
					variables.jobtimer = message.data.jobtimer;
					variables.JP = message.data.JP;
					variables.kamishcharge = message.data.kamishcharge;
					variables.leapjump = message.data.leapjump;
					variables.leftpunch = message.data.leftpunch;
					variables.Mana = message.data.Mana;
					variables.MP = message.data.MP;
					variables.paralyzenot = message.data.paralyzenot;
					variables.PhantomName = message.data.PhantomName;
					variables.punishment = message.data.punishment;
					variables.questinfo = message.data.questinfo;
					variables.QuestProgression = message.data.QuestProgression;
					variables.radius1 = message.data.radius1;
					variables.rushattack = message.data.rushattack;
					variables.shieldbash = message.data.shieldbash;
					variables.Skillcycle = message.data.Skillcycle;
					variables.slashfur = message.data.slashfur;
					variables.slashfurrybroad = message.data.slashfurrybroad;
					variables.slashfurtimer = message.data.slashfurtimer;
					variables.spiderstat = message.data.spiderstat;
					variables.tpd = message.data.tpd;
					variables.TX = message.data.TX;
					variables.TY = message.data.TY;
					variables.TZ = message.data.TZ;
					variables.upforceslash = message.data.upforceslash;
					variables.wp = message.data.wp;
					variables.rangerleapnum = message.data.rangerleapnum;
					variables.rangerleaptimer = message.data.rangerleaptimer;
					variables.sl_EVA = message.data.sl_EVA;
					variables.randplayerx = message.data.randplayerx;
					variables.randplayery = message.data.randplayery;
					variables.randplayerz = message.data.randplayerz;
					variables.traintype = message.data.traintype;
					variables.isdailytraining = message.data.isdailytraining;
					variables.shopitem1 = message.data.shopitem1;
					variables.shopitem2 = message.data.shopitem2;
					variables.shopitem3 = message.data.shopitem3;
					variables.shopitem4 = message.data.shopitem4;
					variables.shopitem6 = message.data.shopitem6;
					variables.Ab1 = message.data.Ab1;
					variables.Ab2 = message.data.Ab2;
					variables.Ab3 = message.data.Ab3;
					variables.Ab4 = message.data.Ab4;
					variables.abilities = message.data.abilities;
					variables.ActiveDaily = message.data.ActiveDaily;
					variables.alivestatus = message.data.alivestatus;
					variables.beru = message.data.beru;
					variables.berumax = message.data.berumax;
					variables.boss = message.data.boss;
					variables.BossKilled = message.data.BossKilled;
					variables.Call4Death = message.data.Call4Death;
					variables.Classes = message.data.Classes;
					variables.combatmode = message.data.combatmode;
					variables.commanddeath = message.data.commanddeath;
					variables.dailykilltyppe = message.data.dailykilltyppe;
					variables.dailysecrettrans = message.data.dailysecrettrans;
					variables.dailytasks = message.data.dailytasks;
					variables.dailytimer = message.data.dailytimer;
					variables.DeathX = message.data.DeathX;
					variables.DeathY = message.data.DeathY;
					variables.DeathZ = message.data.DeathZ;
					variables.Dialogue = message.data.Dialogue;
					variables.domain = message.data.domain;
					variables.dungeoning = message.data.dungeoning;
					variables.DungeonNum = message.data.DungeonNum;
					variables.DunX = message.data.DunX;
					variables.DunY = message.data.DunY;
					variables.DunZ = message.data.DunZ;
					variables.Durability = message.data.Durability;
					variables.giftstatus = message.data.giftstatus;
					variables.GobShadow = message.data.GobShadow;
					variables.GobShadowMax = message.data.GobShadowMax;
					variables.golds = message.data.golds;
					variables.guardbar = message.data.guardbar;
					variables.GuildCode = message.data.GuildCode;
					variables.HunterEyes = message.data.HunterEyes;
					variables.HunterRank = message.data.HunterRank;
					variables.igris = message.data.igris;
					variables.IgrisSpawned = message.data.IgrisSpawned;
					variables.Intelligence = message.data.Intelligence;
					variables.investvalue = message.data.investvalue;
					variables.JOB = message.data.JOB;
					variables.jobkey = message.data.jobkey;
					variables.killmission = message.data.killmission;
					variables.LastKilled = message.data.LastKilled;
					variables.Level = message.data.Level;
					variables.MainQuest = message.data.MainQuest;
					variables.manaregen = message.data.manaregen;
					variables.MaxXP = message.data.MaxXP;
					variables.orcmax = message.data.orcmax;
					variables.orcspawned = message.data.orcspawned;
					variables.OrdShadow = message.data.OrdShadow;
					variables.ordshadowmax = message.data.ordshadowmax;
					variables.overridefeet = message.data.overridefeet;
					variables.overridehead = message.data.overridehead;
					variables.overridelegs = message.data.overridelegs;
					variables.overridetorso = message.data.overridetorso;
					variables.perception = message.data.perception;
					variables.Player = message.data.Player;
					variables.polarbear = message.data.polarbear;
					variables.polarbearmax = message.data.polarbearmax;
					variables.pushup = message.data.pushup;
					variables.ranking = message.data.ranking;
					variables.rankingnum = message.data.rankingnum;
					variables.resistance = message.data.resistance;
					variables.RUN = message.data.RUN;
					variables.RX = message.data.RX;
					variables.RZ = message.data.RZ;
					variables.ShadowExchange = message.data.ShadowExchange;
					variables.ShadowSelect = message.data.ShadowSelect;
					variables.shadowstorage = message.data.shadowstorage;
					variables.shadowstorageusage = message.data.shadowstorageusage;
					variables.situp = message.data.situp;
					variables.SkillPoints = message.data.SkillPoints;
					variables.slashfury = message.data.slashfury;
					variables.Speed = message.data.Speed;
					variables.speedpercent = message.data.speedpercent;
					variables.squat = message.data.squat;
					variables.statshown = message.data.statshown;
					variables.Strength = message.data.Strength;
					variables.summonlimit = message.data.summonlimit;
					variables.summonlimitusage = message.data.summonlimitusage;
					variables.tj = message.data.tj;
					variables.tjonoff = message.data.tjonoff;
					variables.Vitality = message.data.Vitality;
					variables.WolfShadow = message.data.WolfShadow;
					variables.WolfShadowMax = message.data.WolfShadowMax;
					variables.Xp = message.data.Xp;
					variables.xpmultiplier = message.data.xpmultiplier;
					variables.Money = message.data.Money;
					variables.CustomHUD = message.data.CustomHUD;
					variables.ShadowGoblinArcherAmount = message.data.ShadowGoblinArcherAmount;
					variables.ShadowGoblinMageAmount = message.data.ShadowGoblinMageAmount;
					variables.ShadowGoblinArcherMax = message.data.ShadowGoblinArcherMax;
					variables.ShadowGoblinMageMax = message.data.ShadowGoblinMageMax;
					variables.shadowdragonnum = message.data.shadowdragonnum;
					variables.shadowdragonmax = message.data.shadowdragonmax;
					variables.packetCounter = message.data.packetCounter;
					variables.instance_query_timer = message.data.instance_query_timer;
					variables.daily_refreshes = message.data.daily_refreshes;
					variables.selection = message.data.selection;
					variables.party = message.data.party;
					variables.monarchbeam = message.data.monarchbeam;
					variables.baranlightningstrike = message.data.baranlightningstrike;
					variables.prevRank = message.data.prevRank;
					variables.prevLevel = message.data.prevLevel;
					variables.idcd = message.data.idcd;
					variables.title = message.data.title;
					variables.Plist = message.data.Plist;
					variables.Pslot1 = message.data.Pslot1;
					variables.Pslot2 = message.data.Pslot2;
					variables.Pslot3 = message.data.Pslot3;
					variables.Pslot4 = message.data.Pslot4;
					variables.Pslot5 = message.data.Pslot5;
					variables.Pslot6 = message.data.Pslot6;
					variables.Pslot7 = message.data.Pslot7;
					variables.Pslot8 = message.data.Pslot8;
					variables.PslotSelecting = message.data.PslotSelecting;
					variables.PselectedPower = message.data.PselectedPower;
					variables.progression_assassin = message.data.progression_assassin;
					variables.progression_mage = message.data.progression_mage;
					variables.progression_fighter = message.data.progression_fighter;
					variables.progression_tanker = message.data.progression_tanker;
					variables.progression_healer = message.data.progression_healer;
					variables.progression_ranger = message.data.progression_ranger;
					variables.JobSkills = message.data.JobSkills;
					variables.ExchangeDimensions = message.data.ExchangeDimensions;
					variables.ExchangeCords = message.data.ExchangeCords;
					variables.ShadowBody = message.data.ShadowBody;
					variables.progression_multiplier_assassin = message.data.progression_multiplier_assassin;
					variables.progression_multiplier_mage = message.data.progression_multiplier_mage;
					variables.progression_multiplier_fighter = message.data.progression_multiplier_fighter;
					variables.progression_multiplier_tanker = message.data.progression_multiplier_tanker;
					variables.progression_multiplier_healer = message.data.progression_multiplier_healer;
					variables.progression_multiplier_ranger = message.data.progression_multiplier_ranger;
					variables.overlay_alpha_welcome = message.data.overlay_alpha_welcome;
					variables.progression_multiplier_dagger = message.data.progression_multiplier_dagger;
					variables.progression_dagger = message.data.progression_dagger;
					variables.overlay_alpha_dailyquestwarning = message.data.overlay_alpha_dailyquestwarning;
					variables.dkc_unlocked = message.data.dkc_unlocked;
					variables.unlocked_quests = message.data.unlocked_quests;
					variables.finished_quests = message.data.finished_quests;
					variables.highorcmax = message.data.highorcmax;
					variables.highorcspawned = message.data.highorcspawned;
					variables.tuskmax = message.data.tuskmax;
					variables.tuskspawned = message.data.tuskspawned;
					variables.FireRingTimer = message.data.FireRingTimer;
					variables.reward_1 = message.data.reward_1;
					variables.reward_2 = message.data.reward_2;
					variables.reward_3 = message.data.reward_3;
					variables.dkc_cleared = message.data.dkc_cleared;
				}
			});
			context.setPacketHandled(true);
		}
	}
}
