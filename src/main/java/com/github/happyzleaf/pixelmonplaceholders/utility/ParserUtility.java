package com.github.happyzleaf.pixelmonplaceholders.utility;

import com.github.happyzleaf.pixelmonplaceholders.PPConfig;
import com.mojang.datafixers.types.templates.CompoundList;
import com.pixelmonmod.api.pokemon.PokemonSpecification;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.TickHandler;
import com.pixelmonmod.pixelmon.TimeHandler;
import com.pixelmonmod.pixelmon.api.config.BreedingConfig;
import com.pixelmonmod.pixelmon.api.config.GeneralConfig;
import com.pixelmonmod.pixelmon.api.pokemon.*;

import com.pixelmonmod.pixelmon.api.pokemon.ability.Ability;
import com.pixelmonmod.pixelmon.api.pokemon.drops.PokemonDropInformation;
import com.pixelmonmod.pixelmon.api.pokemon.species.Species;
import com.pixelmonmod.pixelmon.api.pokemon.species.Stats;

import com.pixelmonmod.pixelmon.api.pokemon.species.abilities.Abilities;
import com.pixelmonmod.pixelmon.api.pokemon.species.gender.Gender;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.api.pokemon.stats.Moveset;
import com.pixelmonmod.pixelmon.api.pokemon.stats.evolution.Evolution;
import com.pixelmonmod.pixelmon.api.pokemon.stats.evolution.conditions.*;
import com.pixelmonmod.pixelmon.api.pokemon.stats.extraStats.LakeTrioStats;
import com.pixelmonmod.pixelmon.api.pokemon.stats.extraStats.MeltanStats;
import com.pixelmonmod.pixelmon.api.pokemon.stats.extraStats.MewStats;
import com.pixelmonmod.pixelmon.api.registries.PixelmonForms;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.api.util.ITranslatable;
import com.pixelmonmod.pixelmon.api.util.helpers.RandomHelper;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.basic.HiddenPower;

import com.pixelmonmod.pixelmon.client.gui.ScreenHelper;
import com.pixelmonmod.pixelmon.entities.SpawnLocationType;
import com.pixelmonmod.pixelmon.entities.npcs.registry.DropItemRegistry;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;

import com.pixelmonmod.pixelmon.items.HeldItem;
import me.rojo8399.placeholderapi.NoValueException;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;

import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.api.entity.Entity;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ParserUtility {

	private static Field mainDrop_f, rareDrop_f, optDrop1_f, optDrop2_f;

	static {
		try {
			mainDrop_f = PokemonDropInformation.class.getDeclaredField("mainDrop");
			mainDrop_f.setAccessible(true);
			rareDrop_f = PokemonDropInformation.class.getDeclaredField("rareDrop");
			rareDrop_f.setAccessible(true);
			optDrop1_f = PokemonDropInformation.class.getDeclaredField("optDrop1");
			optDrop1_f.setAccessible(true);
			optDrop2_f = PokemonDropInformation.class.getDeclaredField("optDrop2");
			optDrop2_f.setAccessible(true);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param from inclusive
	 * @param to   exclusive
	 */
	public static <T> T[] copyOfRange(T[] original, int from, int to) {
		if (original.length == to - from) {
			try {
				return (T[]) original.getClass().newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
				return null;
			}
		} else {
			return Arrays.copyOfRange(original, from, to);
		}
	}

	public static Object parsePokedexInfo(Species species, @Nullable Stats stats, String[] values) throws NoValueException {
		if (values.length == 0) {
			return species.getLocalizedName();
		}


		switch (values[0]) {
			case "name":
				return species.getLocalizedName();
			case "catchrate":
				return stats.getCatchRate();
			case "nationalid":
				return species.getDex();
			case "rarity": // TODO add
				throw new NoValueException("rarity has been disabled for now");
				/*
				if (values.length == 2) {
					int rarity;
					switch (values[1]) {
						case "day":
							rarity = stats.rarity.day;
							break;
						case "night":
							rarity = stats.rarity.night;
							break;
						case "dawndusk":
							rarity = stats.rarity.dawndusk;
							break;
						default:
							throw new NoValueException();
					}
					return rarity <= 0 ? EnumPokemon.legendaries.contains(pokemon.name) ? 0 : 1 : rarity;
				}
				break;*/
			case "postevolutions":
				return asReadableList(values, 1, stats.getEvolutions().stream().map(evolution -> evolution.to.create().getSpecies().getLocalizedName()).toArray());
			case "preevolutions":
				return asReadableList(values, 1, Arrays.stream(stats.getPreEvolutions().toArray(new PokemonSpecification[0])).map(o -> species.getDefaultFormNames()).filter(Objects::nonNull).map(strings -> species.getName()).toArray());
			case "evolutions": {
				List<String> evolutions = Arrays.stream(stats.getPreEvolutions().stream().toArray()).map(o -> species.getTranslatedName())
						.filter(Objects::nonNull)
						.map(o -> species.getName())
						.collect(Collectors.toList());
				evolutions.add(species.getLocalizedName());
				evolutions.addAll(
						stats.getEvolutions().stream().map(evolution -> evolution.to.create())
								.map(pokemon -> pokemon.getSpecies().getLocalizedName())
								.collect(Collectors.toList())
				);
				return asReadableList(values, 1, evolutions.toArray());
			}
			case "ability":
				if (values.length > 1) {
					String value1 = values[1];
					int index = value1.equals("1") ? 0 : value1.equals("2") ? 1 : value1.equalsIgnoreCase("h") ? 2 : -1;
					if (index != -1) {
						final Ability[] abilities = stats.getAbilities().getAll();
						return index >= abilities.length ? PPConfig.noneText : Arrays.stream(abilities).toArray();
					}
					throwWrongInput("1", "2", "h");
				} else {
					throw new NoValueException("Not enough arguments.");
				}
			case "abilities":
				return asReadableList(values, 1, stats.getAbilities().getAll());
			case "biomes": // TODO add
				throw new NoValueException("biomes have been disabled for now");
				//return asReadableList(values, 2, Arrays.stream(stats.biomeIDs).map(id -> Biome.getBiome(id).getBiomeName()).toArray());
			case "spawnlocations":
				return asReadableList(values, 1, Arrays.stream(stats.getSpawn().getSpawnLocations()).map(SpawnLocationType::getLocalizedName).toArray());
			case "doesevolve":
				return stats.getEvolutions().size() != 0;
			case "evolutionscount":
				return stats.getEvolutions().size();
			case "evolution":
				if (values.length > 1) {
					int evolution;
					try {
						evolution = Integer.parseInt(values[1]) - 1;
					} catch (NumberFormatException e) {
						throw new NoValueException();
					}
					if (stats.getEvolutions().size() <= 0) {
						throw new NoValueException();
					}
					if (stats.getEvolutions().size() <= evolution) {
						return "Does not evolve.";
					} else {
						Evolution evol = stats.getEvolutions().get(evolution);
						if (values.length < 3) {
							return stats.getEvolutions().get(evolution).to.create().getSpecies().getLocalizedName();
						} else {
							String choice = values[2];
							if (choice.equals("list")) { //TODO write better
								List<String> conditions = new ArrayList<>();
								for (Map.Entry<String, EvoParser> entry : evoParsers.entrySet()) {
									for (EvoCondition cond : evol.conditions) {
										if (cond.getClass().equals(entry.getValue().clazz)) {
											conditions.add(entry.getKey());
										}
									}
								}
								return asReadableList(values, 3, conditions.toArray());
							} else {
								try {
									EvoParser parser = evoParsers.get(values[2]);
									if (parser == null) throw new NoValueException();
									EvoCondition cond = null;
									for (EvoCondition c : evol.conditions) {
										if (c.getClass().equals(parser.clazz)) {
											cond = c;
											break;
										}
									}
									if (cond == null) {
										throw new NoValueException(String.format("The condition %s isn't valid.", values[2]));
									}
									try {
										//noinspection unchecked
										return parser.parse(cond, values, 3);
									} catch (IllegalAccessException e) {
										e.printStackTrace();
									}
								} catch (NoValueException e) {
									return PPConfig.evolutionNotAvailableText;
								}
							}

						}
					}
				}
				break;
			case "type":
				return asReadableList(values, 1, stats.getTypes().stream().map(o -> stats.getName()).toArray());
			case "basestats":
				if (values.length > 1) {
					switch (values[1]) {
						case "hp":
							return stats.getBattleStats().getStat(BattleStatsType.HP);
						case "atk":
							return stats.getBattleStats().getStat(BattleStatsType.ATTACK);
						case "def":
							return stats.getBattleStats().getStat(BattleStatsType.DEFENSE);
						case "spa":
							return stats.getBattleStats().getStat(BattleStatsType.SPECIAL_ATTACK);
						case "spd":
							return stats.getBattleStats().getStat(BattleStatsType.SPECIAL_DEFENSE);
						case "spe":
							return stats.getBattleStats().getStat(BattleStatsType.SPEED);
						case "yield":
							if (values.length > 2) {
								switch (values[2]) {
									case "hp":
										return stats.getEVYields().getYield(BattleStatsType.HP);
									case "atk":
										return stats.getEVYields().getYield(BattleStatsType.ATTACK);
									case "def":
										return stats.getEVYields().getYield(BattleStatsType.DEFENSE);
									case "spa":
										return stats.getEVYields().getYield(BattleStatsType.SPECIAL_ATTACK);
									case "spd":
										return stats.getEVYields().getYield(BattleStatsType.SPECIAL_DEFENSE);
									case "spe":
										return stats.getEVYields().getYield(BattleStatsType.SPEED);
									default:
										throwWrongInput("hp", "atk", "def", "spa", "spd", "spe");
								}
							}
							break;
						case "yields":
							return Arrays.stream(stats.getEVYields().toArray()).sum();
						default:
							throwWrongInput("hp", "atk", "def", "spa", "spd", "spe", "yield", "yields");
					}
				}
				break;
			case "drops":
				if (values.length > 1) {
					Set<PokemonDropInformation> drops = DropItemRegistry.pokemonDrops.get(species);
					if (drops == null) {
						return PPConfig.noneText;
					} else {
						switch (values[1]) {
							case "main":
								return getDropsInfo(values, 2, drops, mainDrop_f);
							case "rare":
								return getDropsInfo(values, 2, drops, rareDrop_f);
							case "optional1":
								return getDropsInfo(values, 2, drops, optDrop1_f);
							case "optional2":
								return getDropsInfo(values, 2, drops, optDrop2_f);
							default:
								throwWrongInput("main", "rare", "optional1", "optional2");
						}
					}
				}
				break;
			case "egggroups":
				return asReadableList(values, 1, stats.getEggGroups().toArray(new EggGroup[0]));
			case "texturelocation":
				return "pixelmon:" + ScreenHelper.getPokemonSprite();
			case "move":
				if (values.length > 1) {
					try {
						Object[] attacks = getAllAttackNames(stats);
						int attack = Integer.parseInt(values[1]) - 1;
						if (attack >= 0 && attack < attacks.length) {
							return attacks[attack];
						} else {
							return PPConfig.moveNotAvailableText;
						}
					} catch (NumberFormatException ignored) {
					}
				}
				break;
			case "moves":
				return asReadableList(values, 1, getAllAttackNames(stats));
			case "islegend":
				return species.isLegendary();
			case "isub":
				return species.isUltraBeast();
		}
		throw new NoValueException();
	}

	public static Object throwWrongInput(Object... expectedValues) throws NoValueException {
		throw new NoValueException("Wrong input." + (expectedValues.length > 0 ? " Expected values: " + Arrays.toString(expectedValues) : ""));
	}

	public static boolean checkSpecies(String name, Pokemon pokemon, PixelmonSpecies... species) throws NoValueException {
		if (Arrays.stream(species).noneMatch(s -> pokemon.isPokemon())) {
			throw new NoValueException(String.format("Wrong input. '%s' can only be used by %s.", name, Arrays.toString(species)));
		}

		return true;
	}


	public static Object[] getAllAttackNames(Stats stats) {
		return stats.getMoves().getAllMoves().stream().map(attack -> attack.getAttackType().getLocalizedName()).toArray();
	}

	public static Object parsePokemonInfo(Entity owner, Pokemon pokemon, String[] values) throws NoValueException {
		if (pokemon == null) {
			return PPConfig.teamMemberNotAvailableText;
		}

		if (PPConfig.disableEggInfo && pokemon.isEgg()) {
			if (values.length != 1 || !values[0].equals("texturelocation")) {
				return PPConfig.disabledEggText;
			}
		}

		if (values.length > 0) {
			switch (values[0]) {
				case "nickname":
					return pokemon.getDisplayName();
				case "exp":
					return formatBigNumbers(pokemon.getExperience());
				case "level":
					return pokemon.getPokemonLevel();
				case "exptolevelup":
					return formatBigNumbers(pokemon.getExperienceToLevelUp());
				case "stats":
					if (values.length > 1) {
						switch (values[1]) {
							case "hp":
								return pokemon.getStats().getHP();
							case "atk":
								return pokemon.getStats().getAttack();
							case "def":
								return pokemon.getStats().getDefense();
							case "spa":
								return pokemon.getStats().getSpecialAttack();
							case "spd":
								return pokemon.getStats().getSpecialDefense();
							case "spe":
								return pokemon.getStats().getSpeed();
							case "ivs":
								if (values.length > 2) {
									switch (values[2]) {
										case "hp":
											return pokemon.getIVs().getStat(BattleStatsType.HP);
										case "atk":
											return pokemon.getIVs().getStat(BattleStatsType.ATTACK);
										case "def":
											return pokemon.getIVs().getStat(BattleStatsType.DEFENSE);
										case "spa":
											return pokemon.getIVs().getStat(BattleStatsType.SPECIAL_ATTACK);
										case "spd":
											return pokemon.getIVs().getStat(BattleStatsType.SPECIAL_DEFENSE);
										case "spe":
											return pokemon.getIVs().getStat(BattleStatsType.SPEED);
										case "total":
											return Arrays.stream(pokemon.getStats().getIVs().getArray()).sum();
										case "totalpercentage":
											return formatDouble((Arrays.stream(pokemon.getStats().getIVs().getArray()).sum()) * 100 / 186d);
										default:
											throwWrongInput("hp", "atk", "def", "spa", "spd", "spe", "total", "totalpercentage");
									}
								}
								break;
							case "evs":
								if (values.length > 2) {
									switch (values[2]) {
										case "hp":
											return pokemon.getEVs().getStat(BattleStatsType.HP);
										case "atk":
											return pokemon.getEVs().getStat(BattleStatsType.ATTACK);
										case "def":
											return pokemon.getEVs().getStat(BattleStatsType.DEFENSE);
										case "spa":
											return pokemon.getEVs().getStat(BattleStatsType.SPECIAL_ATTACK);
										case "spd":
											return pokemon.getEVs().getStat(BattleStatsType.SPECIAL_DEFENSE);
										case "spe":
											return pokemon.getEVs().getStat(BattleStatsType.SPEED);
										case "total":
											return Arrays.stream(pokemon.getStats().getEVs().getArray()).sum();
										case "totalpercentage":
											return formatDouble(Arrays.stream(pokemon.getStats().getEVs().getArray()).sum() * 100 / 510d);
										default:
											throwWrongInput("hp", "atk", "def", "spa", "spd", "spe", "total", "totalpercentage");
									}
								}
								break;
							default:
								throwWrongInput("hp", "atk", "def", "spa", "spd", "spe", "ivs", "evs");
						}
					}
					break;
				case "helditem":
					return pokemon.getHeldItem() == ItemStack.EMPTY ? PPConfig.noneText : pokemon.getHeldItem().getDisplayName();
				case "pos":
					if (values.length > 1) {
						PixelmonEntity entity = pokemon.ifEntityExists(pixelmonEntity -> );
						BlockPos pos = entity == null ? ((net.minecraft.entity.Entity) owner).getPosition() : entity.getPosition();
						switch (values[1]) {
							case "x":
								return pos.getX();
							case "y":
								return pos.getY();
							case "z":
								return pos.getZ();
							default:
								throwWrongInput("x", "y", "z");
						}
					}
					break;
				case "moveset":
					Moveset moveset = pokemon.getMoveset();
					try {
						int moveIndex = Integer.parseInt(values[1]) - 1;
						if (moveIndex < 0 || moveIndex >= 4) {
							throw new NoValueException("The attack index must be between 0 and 4.");
						}
						final Attack attack = moveset.get(moveIndex);
						return attack == null ? PPConfig.noneText : attack.getActualMove().getLocalizedName();
					} catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
						return asReadableList(values, 1,
								Arrays.stream(moveset.attacks)
										.filter(Objects::nonNull)
										.map(attack -> attack.getActualMove().getLocalizedName())
										.toArray());
					}
				case "friendship":
					return formatBigNumbers(pokemon.getFriendship());
				case "ability":
					if (values.length == 1) {
						return pokemon.getAbility().getLocalizedName();
					} else if (values[1].equals("slot")) {
						return pokemon.getForm().getAbilities().getAbilitySlot(pokemon.getAbility()) == 2 ? "H" : pokemon.getForm().getAbilities().getAbilitySlot(pokemon.getAbility()) + 1;
					}

					break;
				case "ball":
					return pokemon.getBall().getName();
				case "possibledrops":
				//	return asReadableList(pokeValues, 1, DropItemRegistry.getDropsForPokemon(pokemon).stream().map(ParserUtility::getItemStackInfo).toArray());
				case "nature": {
					Nature nature = pokemon.getNature();
					if (values.length > 1) {
						switch (values[1]) {
							case "increased":
								return nature.getIncreasedStat() == BattleStatsType.NONE ? PPConfig.noneText : nature.getIncreasedStat().getLocalizedName();
							case "decreased":
								return nature.getDecreasedStat() == BattleStatsType.NONE ? PPConfig.noneText : nature.getIncreasedStat().getLocalizedName();
						}

						throwWrongInput("increased", "decreased");
					}

					return nature.getLocalizedName();
				}
				case "gender":
					return pokemon.getGender().getLocalizedName();
				case "growth":
					return pokemon.getGrowth().getLocalizedName();
				case "shiny":
					return pokemon.isShiny();
				case "hiddenpower":
					return HiddenPower.getHiddenPowerType(pokemon.getStats().ivs).getLocalizedName();
				case "texturelocation": {
					ResourceLocation location;
					if (pokemon.isEgg()) {
						location = GuiResources.getEggSprite(pokemon.getSpecies(), pokemon.getEggCycles());
					} else {
						location = ScreenHelper.getPokemonSprite(pokemon, Minecraft.getInstance());
					}
					return location.toString().replace("textures/", "").replace(".png", "");
				}
				case "customtexture":
					String custom = pokemon.getCustomTexture();
					if (custom != null && !custom.isEmpty()) {
						return custom;
					}
					return PPConfig.noneText;

				case "form":
					return pokemon.getForm();
				case "extraspecs":
					if (values.length > 1) {
						ISpecType spec = PokemonSpec.getSpecForKey(values[1]);
						if (spec instanceof SpecFlag) { //Could move to SpecType<Boolean> but let's imply this is the standard for boolean-based specs.
							return ((SpecFlag) spec).matches(pokemon);
						}

						throw new NoValueException("Spec not supported.");
					}

					throw new NoValueException("Not enough arguments.");
				case "aura":
					return getAuraID(pokemon);
				case "originaltrainer":
					if (values.length > 1) {
						switch (values[1]) {
							case "name":
								return pokemon.getOriginalTrainer() == null ? PPConfig.noneText : pokemon.getOriginalTrainer();
							case "uuid":
								return pokemon.getOriginalTrainerUUID() == null ? PPConfig.noneText : pokemon.getOriginalTrainerUUID();
						}
					}

					throwWrongInput("name", "uuid");
				case "eggsteps": // since 2.1.4
					if (!pokemon.isEgg())
						throw new NoValueException("The pixelmon is not in an egg.");
					if (values.length > 1) {
						int current = pokemon.getEggCycles() * pokemon.getEggSteps();
						switch (values[1]) {
							case "current":
								return current;
							case "needed":
								return pokemon.getStats().getPokemon().getEg
						//pokemon.getBaseStats().getEggCycles() * GeneralConfig.getS - current;
						}
					}

					throwWrongInput("current", "needed");
				case "mew":
					if (checkSpecies("mew", pokemon, PixelmonSpecies.MEW)) {
						if (values.length > 1 && values[1].equals("clones")) {
							MewStats mew = (MewStats) pokemon.getExtraStats();
							if (values.length > 2) {
								switch (values[2]) {
									case "used":
										return mew.numCloned == MewStats.MAX_CLONES;
									case "total":
										return mew.numCloned;
								}
							}

							return mew.numCloned + "/" + MewStats.MAX_CLONES;
						}

						throwWrongInput("clones");
					}
				case "laketrio":
					if (checkSpecies("laketrio", pokemon, sp, PixelmonSpecies.MESPRIT, PixelmonSpecies.AZELF)) {
						if (values.length > 1 && values[1].equals("rubies")) {
							LakeTrioStats lakeTrio = (LakeTrioStats) pokemon.getExtraStats();
							if (values.length > 2) {
								switch (values[2]) {
									case "used":
										return lakeTrio.numEnchanted == GeneralConfig.getLakeTrioMaxEnchants();
									case "total":
										return lakeTrio.numEnchanted;
								}
							}

							return lakeTrio.numEnchanted + "/" + Pixelmon;
						}

						throwWrongInput("rubies");
					}
				case "meltan":
					if (checkSpecies("meltan", pokemon, PixelmonSpecies.MELTAN)) {
						MeltanStats meltan = (MeltanStats) pokemon.getExtraStats();
						if (values.length > 1 && values[1].equals("oressmelted")) {
							if (values.length > 2) {
								switch (values[2]) {
									case "used":
										return meltan.oresSmelted > 0;
									case "total":
										return meltan.oresSmelted;
								}
							}

							throwWrongInput("used", "total");
						}

						throwWrongInput("oressmelted");
					}
			}
		}

		return parsePokedexInfo(pokemon.getSpecies(), pokemon.getForm(), values);
	}


	/**
	 * @param index The index in the array values where the method should start
	 */
	public static Object asReadableList(String[] values, int index, Object[] data) {
		if (data == null) return "";
		String separator = ", ";
		if (values.length == index + 1) {
			separator = values[index].replaceAll("--", " ");
		}
		String list = "";
		for (Object d : data) {
			if (d == null) continue;
			if (list.isEmpty()) {
				list = d.toString();
			} else {
				list = list.concat(separator + d);
			}
		}
		return list.isEmpty() ? PPConfig.noneText : list;
	}

	public static String normalizeText(String text) {
		return text.substring(1) + text.substring(1, text.length() - 1);
	}

	public static String formatBigNumbers(int number) {
		if (number < 1000) {
			return String.valueOf(number);
		} else if (number < 1000000) {
			return (double) Math.round(number / 100f) / 10 + "k";
		} else if (number < 1000000000) {
			return (double) Math.round(number / 100000f) / 10 + "m";
		} else {
			return (double) Math.round(number / 100000000f) / 10 + "b";
		}
	}

	private static final DecimalFormat formatter = new DecimalFormat();

	static {
		formatter.setMaximumFractionDigits(PPConfig.maxFractionDigits);
		formatter.setMinimumFractionDigits(PPConfig.minFractionDigits);
	}

	public static String formatDouble(double number) {
		return formatter.format(number);
	}

	public static Object getDropsInfo(String[] values, int index, Set<PokemonDropInformation> drops, Field variantField) {
		try {
			List<Object> results = new ArrayList<>();
			for (PokemonDropInformation drop : drops) {
				final ItemStack dropItem = (ItemStack) variantField.get(drop);
				results.add(getItemStackInfo(dropItem));
			}
			return asReadableList(values, index, results.toArray());
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static Object getItemStackInfo(@Nullable ItemStack is) {
		return is == null || is.getCount() == 0 ? PPConfig.noneText : is.getCount() + " " + is.getDisplayName();
	}

	private static Map<String, EvoParser> evoParsers = new HashMap<>();

	static {
		evoParsers.put("biome", new EvoParser<BiomeCondition>(BiomeCondition.class) {
			@Override
			public Object parse(BiomeCondition condition, String[] values, int index) { //TODO CACHE!
				return asReadableList(values, index, condition.biomes.stream().map(biome -> ForgeRegistries.BIOMES.getKeys().stream().filter(b -> biome.equalsIgnoreCase(b.getNamespace())).findFirst().get()).toArray());
			}
		});
		evoParsers.put("chance", new EvoParser<ChanceCondition>(ChanceCondition.class) {
			@Override
			public Object parse(ChanceCondition condition, String[] values, int index) {
				return formatDouble(condition.chance);
			}
		});
		evoParsers.put("stone", new EvoParser<EvoRockCondition>(EvoRockCondition.class) {
			@Override
			public Object parse(EvoRockCondition condition, String[] values, int index) throws NoValueException {
				if (values.length > index) {
					if (values[index].equals("biome")) {
						return asReadableList(values, index + 1, Arrays.stream(condition.evolutionRock.getBiomes()).map(biome -> biome.getName()).filter(Objects::nonNull).toArray());
					} else {
						throw new NoValueException();
					}
				}
				return condition.evolutionRock;
			}
		});
		evoParsers.put("friendship", new EvoParser<FriendshipCondition>(FriendshipCondition.class) {
			@Override
			public Object parse(FriendshipCondition condition, String[] values, int index) throws IllegalAccessException {
				return condition.friendship == -1 ? 220 : condition.friendship;
			}
		});
		evoParsers.put("gender", new EvoParser<GenderCondition>(GenderCondition.class) {
			@Override
			public Object parse(GenderCondition condition, String[] values, int index) {
				return asReadableList(values, index, condition.genders.stream().map(Gender::getLocalizedName).toArray());
			}
		});
		evoParsers.put("helditem", new EvoParser<HeldItemCondition>(HeldItemCondition.class) {
			@Override
			public Object parse(HeldItemCondition condition, String[] values, int index) { //TODO test
				return ((HeldItem) ForgeRegistries.ITEMS.getValue(new ResourceLocation(condition.item.itemID))).getLocalizedName();
			}
		});
		evoParsers.put("altitude", new EvoParser<HighAltitudeCondition>(HighAltitudeCondition.class) {
			@Override
			public Object parse(HighAltitudeCondition condition, String[] values, int index) {
				return formatDouble(condition.minAltitude);
			}
		});
		evoParsers.put("level", new EvoParser<LevelCondition>(LevelCondition.class) {
			@Override
			public Object parse(LevelCondition condition, String[] values, int index) throws IllegalAccessException {
				return condition.level;
			}
		});
		evoParsers.put("move", new EvoParser<>(MoveCondition.class) {
			@Override
			public Object parse(MoveCondition condition, String[] values, int index) throws NoValueException, IllegalAccessException {
				return null;
			}
			/*@Override
			public Object parse(MoveCondition condition, String[] values, int index) { //TODO test
				return Attack.getAttacks(condition.).map(attackBase -> (Object) attackBase.getLocalizedName()).orElse(PPConfig.noneText);
			}
			*/ //TODO replace
		});
		evoParsers.put("movetype", new EvoParser<MoveTypeCondition>(MoveTypeCondition.class) {
			@Override
			public Object parse(MoveTypeCondition condition, String[] values, int index) throws IllegalAccessException {
				return condition.type.getLocalizedName();
			}
		});
		evoParsers.put("ore", new EvoParser<OreCondition>(OreCondition.class) {
			@Override
			public Object parse(OreCondition condition, String[] values, int index) throws NoValueException, IllegalAccessException {
				return condition.ores;
			}
		});
//		evoParsers.put("partyalolan", new EvoParser<PartyAlolanCondition>(PartyAlolanCondition.class) {
//			@Override
//			public Object parse(PartyAlolanCondition condition, String[] values, int index) throws NoValueException, IllegalAccessException {
//				throw new NoValueException();
//			}
//		});

		//TODO Replaced the 'pokemonSpecification -> null' with something that works as this gonna break
		evoParsers.put("party", new EvoParser<PartyCondition>(PartyCondition.class) {
			@Override
			public Object parse(PartyCondition condition, String[] values, int index) throws NoValueException {
				if (values.length > index) {
					if (values[index].equals("pokemon")) {
						return asReadableList(values, index + 1, condition.withPokemon.stream().map(pokemonSpecification -> null).toArray());
					} else if (values[index].equals("type")) {
						return asReadableList(values, index + 1, condition.withTypes.stream().map(Element::getLocalizedName).toArray());
					}
				}
				throw new NoValueException();
			}
		});
		evoParsers.put("stats", new EvoParser<StatRatioCondition>(StatRatioCondition.class) {
			@Override
			public Object parse(StatRatioCondition condition, String[] values, int index) throws NoValueException, IllegalAccessException {
				if (values.length > index) {
					switch (values[index]) {
						case "ratio":
							return formatDouble(condition.ratio);
						case "1":
							return condition.stat1.getLocalizedName();
						case "2":
							return condition.stat2.getLocalizedName();
					}
				}
				throw new NoValueException();
			}
		});
		evoParsers.put("time", new EvoParser<TimeCondition>(TimeCondition.class) { //TODO fix
			@Override
			public Object parse(TimeCondition condition, String[] values, int index) throws NoValueException {
				return normalizeText(condition.time.getLocalizedName());
			}
		});
		evoParsers.put("weather", new EvoParser<WeatherCondition>(WeatherCondition.class) {
			@Override
			public Object parse(WeatherCondition condition, String[] values, int index) throws IllegalAccessException {
				return condition.weather.getLocalizedName();
			}
		});
	}

	public static abstract class EvoParser<T extends EvoCondition> {
		public Class<T> clazz;

		public EvoParser(Class<T> clazz) {
			this.clazz = clazz;
		}

		public abstract Object parse(T condition, String[] values, int index) throws NoValueException, IllegalAccessException;
	}
}
