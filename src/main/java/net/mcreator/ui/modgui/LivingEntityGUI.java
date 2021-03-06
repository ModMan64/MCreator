/*
 * MCreator (https://mcreator.net/)
 * Copyright (C) 2020 Pylo and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.mcreator.ui.modgui;

import net.mcreator.blockly.BlocklyCompileNote;
import net.mcreator.blockly.BlocklyToAITasks;
import net.mcreator.blockly.data.Dependency;
import net.mcreator.blockly.data.BlocklyLoader;
import net.mcreator.blockly.data.ExternalBlockLoader;
import net.mcreator.blockly.data.ToolboxBlock;
import net.mcreator.element.GeneratableElement;
import net.mcreator.element.ModElementType;
import net.mcreator.element.parts.Particle;
import net.mcreator.element.parts.TabEntry;
import net.mcreator.element.types.GUI;
import net.mcreator.element.types.Mob;
import net.mcreator.generator.blockly.BlocklyBlockCodeGenerator;
import net.mcreator.generator.blockly.ProceduralBlockCodeGenerator;
import net.mcreator.generator.template.TemplateGeneratorException;
import net.mcreator.minecraft.DataListEntry;
import net.mcreator.minecraft.ElementUtil;
import net.mcreator.ui.MCreator;
import net.mcreator.ui.MCreatorApplication;
import net.mcreator.ui.blockly.AITasksEditorToolbar;
import net.mcreator.ui.blockly.BlocklyPanel;
import net.mcreator.ui.blockly.CompileNotesPanel;
import net.mcreator.ui.component.JColor;
import net.mcreator.ui.component.JEmptyBox;
import net.mcreator.ui.component.SearchableComboBox;
import net.mcreator.ui.component.util.ComboBoxUtil;
import net.mcreator.ui.component.util.ComponentUtils;
import net.mcreator.ui.component.util.PanelUtils;
import net.mcreator.ui.dialogs.TextureImportDialogs;
import net.mcreator.ui.help.HelpUtils;
import net.mcreator.ui.init.UIRES;
import net.mcreator.ui.laf.renderer.ModelComboBoxRenderer;
import net.mcreator.ui.laf.renderer.WTextureComboBoxRenderer;
import net.mcreator.ui.minecraft.*;
import net.mcreator.ui.validation.AggregatedValidationResult;
import net.mcreator.ui.validation.Validator;
import net.mcreator.ui.validation.component.VComboBox;
import net.mcreator.ui.validation.component.VTextField;
import net.mcreator.ui.validation.validators.TextFieldValidator;
import net.mcreator.util.ListUtils;
import net.mcreator.util.StringUtils;
import net.mcreator.workspace.elements.ModElement;
import net.mcreator.workspace.elements.VariableElementType;
import net.mcreator.workspace.resources.Model;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class LivingEntityGUI extends ModElementGUI<Mob> {

	private ProcedureSelector onStruckByLightning;
	private ProcedureSelector whenMobFalls;
	private ProcedureSelector whenMobDies;
	private ProcedureSelector whenMobIsHurt;
	private ProcedureSelector onRightClickedOn;
	private ProcedureSelector whenThisMobKillsAnother;
	private ProcedureSelector onMobTickUpdate;
	private ProcedureSelector onPlayerCollidesWith;
	private ProcedureSelector onInitialSpawn;

	private ProcedureSelector particleCondition;
	private ProcedureSelector spawningCondition;

	private final SoundSelector livingSound = new SoundSelector(mcreator);
	private final SoundSelector hurtSound = new SoundSelector(mcreator);
	private final SoundSelector deathSound = new SoundSelector(mcreator);
	private final SoundSelector stepSound = new SoundSelector(mcreator);

	private final VTextField mobName = new VTextField();

	private final JSpinner attackStrength = new JSpinner(new SpinnerNumberModel(3, 0, 10000, 1));
	private final JSpinner movementSpeed = new JSpinner(new SpinnerNumberModel(0.3, 0, 50, 0.1));
	private final JSpinner armorBaseValue = new JSpinner(new SpinnerNumberModel(0.0, 0, 100, 0.1));
	private final JSpinner health = new JSpinner(new SpinnerNumberModel(10, 0, 1024, 1));
	private final JSpinner knockbackResistance = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 0.1));
	private final JSpinner attackKnockback = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 0.1));

	private final JSpinner trackingRange = new JSpinner(new SpinnerNumberModel(64, 0, 10000, 1));

	private final JSpinner spawningProbability = new JSpinner(new SpinnerNumberModel(20, 1, 1000, 1));
	private final JSpinner minNumberOfMobsPerGroup = new JSpinner(new SpinnerNumberModel(4, 1, 1000, 1));
	private final JSpinner maxNumberOfMobsPerGroup = new JSpinner(new SpinnerNumberModel(4, 1, 1000, 1));

	private final JSpinner modelWidth = new JSpinner(new SpinnerNumberModel(0.6, 0, 1024, 0.1));
	private final JSpinner modelHeight = new JSpinner(new SpinnerNumberModel(1.8, 0, 1024, 0.1));
	private final JSpinner mountedYOffset = new JSpinner(new SpinnerNumberModel(0, -1024, 1024, 0.1));
	private final JSpinner modelShadowSize = new JSpinner(new SpinnerNumberModel(0.5, 0, 20, 0.1));
	private final JCheckBox disableCollisions = new JCheckBox("Disable collision box");

	private final JSpinner xpAmount = new JSpinner(new SpinnerNumberModel(0, 0, 100000, 1));

	private final JCheckBox hasAI = new JCheckBox(
			"<html><b>Enable AI</b><br><small>When using AI, make sure to have some AI tasks defined below");
	private final JCheckBox isBoss = new JCheckBox();

	private final JCheckBox immuneToFire = new JCheckBox("Fire");
	private final JCheckBox immuneToArrows = new JCheckBox("Arrows");
	private final JCheckBox immuneToFallDamage = new JCheckBox("Fall damage");
	private final JCheckBox immuneToCactus = new JCheckBox("Cactus");
	private final JCheckBox immuneToDrowning = new JCheckBox("Drowning");
	private final JCheckBox immuneToLightning = new JCheckBox("Lightning");
	private final JCheckBox immuneToPotions = new JCheckBox("Potions");
	private final JCheckBox immuneToPlayer = new JCheckBox("Direct player attack");

	private final JCheckBox spawnParticles = new JCheckBox("Spawn particles around entity");

	private final JCheckBox waterMob = new JCheckBox(
			"Is water entity (breathes underwater, is not pushed by water flow)");
	private final JCheckBox flyingMob = new JCheckBox(
			"Is flying entity (gravity does not pull it down, does not take fall damage)");

	private final JCheckBox hasSpawnEgg = new JCheckBox();
	private final DataListComboBox creativeTab = new DataListComboBox(mcreator);

	private final JComboBox<String> mobSpawningType = new JComboBox<>(
			new String[] { "monster", "creature", "ambient", "waterCreature" });

	private MCItemHolder mobDrop;
	private MCItemHolder equipmentMainHand;
	private MCItemHolder equipmentHelmet;
	private MCItemHolder equipmentBody;
	private MCItemHolder equipmentLeggings;
	private MCItemHolder equipmentBoots;
	private MCItemHolder equipmentOffHand;

	private final JComboBox<String> guiBoundTo = new JComboBox<>();
	private final JSpinner inventorySize = new JSpinner(new SpinnerNumberModel(9, 0, 256, 1));
	private final JSpinner inventoryStackSize = new JSpinner(new SpinnerNumberModel(64, 1, 1024, 1));

	private MCItemHolder rangedAttackItem;

	private final JComboBox<String> rangedItemType = new JComboBox<>();

	private final JTextField mobLabel = new JTextField();

	private final JCheckBox spawnInDungeons = new JCheckBox("Spawn in dungeons");
	private final JColor spawnEggBaseColor = new JColor(mcreator);
	private final JColor spawnEggDotColor = new JColor(mcreator);

	private static final Model biped = new Model.BuiltInModel("Biped");
	private static final Model chicken = new Model.BuiltInModel("Chicken");
	private static final Model cow = new Model.BuiltInModel("Cow");
	private static final Model creeper = new Model.BuiltInModel("Creeper");
	private static final Model ghast = new Model.BuiltInModel("Ghast");
	private static final Model pig = new Model.BuiltInModel("Pig");
	private static final Model slime = new Model.BuiltInModel("Slime");
	private static final Model spider = new Model.BuiltInModel("Spider");
	private static final Model villager = new Model.BuiltInModel("Villager");
	private static final Model silverfish = new Model.BuiltInModel("Silverfish");
	public static final Model[] builtinmobmodels = new Model[] { biped, chicken, cow, creeper, ghast, pig, slime,
			spider, villager, silverfish };
	private final JComboBox<Model> mobModel = new JComboBox<>();

	private final VComboBox<String> mobModelTexture = new SearchableComboBox<>();
	private final VComboBox<String> mobModelGlowTexture = new SearchableComboBox<>();

	//mob bases
	private final JComboBox<String> aiBase = new JComboBox<>(
			new String[] { "(none)", "Creeper", "Skeleton", "Enderman", "Blaze", "Slime", "Witch", "Zombie",
					"MagmaCube", "Pig", "Villager", "Wolf", "Cow", "Bat", "Chicken", "Ocelot", "Squid", "Horse",
					"Spider", "IronGolem" });

	private final JComboBox<String> mobBehaviourType = new JComboBox<>(new String[] { "Mob", "Creature" });
	private final JComboBox<String> mobCreatureType = new JComboBox<>(
			new String[] { "UNDEFINED", "UNDEAD", "ARTHROPOD", "ILLAGER", "WATER" });
	private final JComboBox<String> bossBarColor = new JComboBox<>(
			new String[] { "PINK", "BLUE", "RED", "GREEN", "YELLOW", "PURPLE", "WHITE" });
	private final JComboBox<String> bossBarType = new JComboBox<>(
			new String[] { "PROGRESS", "NOTCHED_6", "NOTCHED_10", "NOTCHED_12", "NOTCHED_20" });

	private final JComboBox<String> particleToSpawn = new JComboBox<>(ElementUtil.loadParticles());
	private final JComboBox<String> particleSpawningShape = new JComboBox<>(
			new String[] { "Spread", "Top", "Tube", "Plane" });
	private final JSpinner particleSpawningRadious = new JSpinner(new SpinnerNumberModel(0.5, 0, 2, 0.1f));
	private final JSpinner particleAmount = new JSpinner(new SpinnerNumberModel(4, 0, 1000, 1));

	private final JCheckBox ridable = new JCheckBox("Rideable");

	private final JCheckBox canControlForward = new JCheckBox("Forward movement control");
	private final JCheckBox canControlStrafe = new JCheckBox("Strafe movement control");

	private final JCheckBox breedable = new JCheckBox(
			"<html>Check to make this entity <b>animal type</b>" + ", breed items:"
					+ "<br><small>Entity base and behaviour type will be ignored if checked,<br>"
					+ "animal entity type will be used");

	private final JCheckBox tameable = new JCheckBox("<html><b>Can tame</b>");

	private final JCheckBox ranged = new JCheckBox("<html>Check to make entity do <b>ranged</b> attacks."
			+ "<br><small>Add at least one <b>Act aggressively against</b> AI task to define targets."
			+ "<br>You need to select item that will be shot too.");

	private MCItemListField breedTriggerItems;

	private final JCheckBox spawnThisMob = new JCheckBox();
	private final JCheckBox doesDespawnWhenIdle = new JCheckBox();

	private BiomeListField restrictionBiomes;

	private BlocklyPanel blocklyPanel;
	private final CompileNotesPanel compileNotesPanel = new CompileNotesPanel();
	private boolean hasErrors = false;
	private Map<String, ToolboxBlock> externalBlocks;

	private boolean disableMobModelCheckBoxListener = false;

	public LivingEntityGUI(MCreator mcreator, ModElement modElement, boolean editingMode) {
		super(mcreator, modElement, editingMode);
		this.initGUI();
		super.finalizeGUI();
	}

	private void setDefaultAISet() {
		blocklyPanel.setXML("<xml xmlns=\"https://developers.google.com/blockly/xml\">"
				+ "<block type=\"aitasks_container\" deletable=\"false\" x=\"40\" y=\"40\"><next>"
				+ "<block type=\"attack_on_collide\"><field name=\"speed\">1.2</field><field name=\"longmemory\">FALSE</field><next>"
				+ "<block type=\"wander\"><field name=\"speed\">1</field><next>"
				+ "<block type=\"attack_action\"><field name=\"callhelp\">FALSE</field><next>"
				+ "<block type=\"look_around\"><next><block type=\"swim_in_water\"/></next>"
				+ "</block></next></block></next></block></next></block></next></block></xml>");
	}

	private void regenerateAITasks() {
		BlocklyBlockCodeGenerator blocklyBlockCodeGenerator = new BlocklyBlockCodeGenerator(externalBlocks,
				mcreator.getWorkspace().getGenerator().getGeneratorStats().getGeneratorAITasks());

		BlocklyToAITasks blocklyToJava;
		try {
			blocklyToJava = new BlocklyToAITasks(mcreator.getWorkspace(), blocklyPanel.getXML(), null,
					new ProceduralBlockCodeGenerator(blocklyBlockCodeGenerator));
		} catch (TemplateGeneratorException e) {
			return;
		}

		List<BlocklyCompileNote> compileNotesArrayList = blocklyToJava.getCompileNotes();

		SwingUtilities.invokeLater(() -> {
			compileNotesPanel.updateCompileNotes(compileNotesArrayList);
			hasErrors = false;
			for (BlocklyCompileNote note : compileNotesArrayList) {
				if (note.getType() == BlocklyCompileNote.Type.ERROR) {
					hasErrors = true;
					break;
				}
			}
		});
	}

	@Override protected void initGUI() {
		onStruckByLightning = new ProcedureSelector(this.withEntry("entity/when_struck_by_lightning"), mcreator,
				"When it is struck by lightning",
				Dependency.fromString("x:number/y:number/z:number/world:world/entity:entity"));
		whenMobFalls = new ProcedureSelector(this.withEntry("entity/when_falls"), mcreator, "When entity falls",
				Dependency.fromString("x:number/y:number/z:number/world:world/entity:entity"));
		whenMobDies = new ProcedureSelector(this.withEntry("entity/when_dies"), mcreator, "When entity dies",
				Dependency.fromString("x:number/y:number/z:number/world:world/entity:entity/sourceentity:entity"));
		whenMobIsHurt = new ProcedureSelector(this.withEntry("entity/when_hurt"), mcreator, "When entity is hurt",
				Dependency.fromString("x:number/y:number/z:number/world:world/entity:entity/sourceentity:entity"));
		onRightClickedOn = new ProcedureSelector(this.withEntry("entity/when_right_clicked"), mcreator,
				"When right clicked on entity", Dependency.fromString(
				"x:number/y:number/z:number/world:world/entity:entity/sourceentity:entity/itemstack:itemstack"));
		whenThisMobKillsAnother = new ProcedureSelector(this.withEntry("entity/when_kills_another"), mcreator,
				"When this entity kills another one",
				Dependency.fromString("x:number/y:number/z:number/world:world/entity:entity/sourceentity:entity"));
		onMobTickUpdate = new ProcedureSelector(this.withEntry("entity/on_tick_update"), mcreator,
				"On entity tick update", Dependency.fromString("x:number/y:number/z:number/world:world/entity:entity"));
		onPlayerCollidesWith = new ProcedureSelector(this.withEntry("entity/when_player_collides"), mcreator,
				"When player collides with this entity",
				Dependency.fromString("x:number/y:number/z:number/world:world/entity:entity/sourceentity:entity"));
		onInitialSpawn = new ProcedureSelector(this.withEntry("entity/on_initial_spawn"), mcreator,
				"On initial entity spawn",
				Dependency.fromString("x:number/y:number/z:number/world:world/entity:entity"));

		particleCondition = new ProcedureSelector(this.withEntry("entity/particle_condition"), mcreator,
				"Particle spawning condition", ProcedureSelector.Side.CLIENT, true, VariableElementType.LOGIC,
				Dependency.fromString("x:number/y:number/z:number/world:world/entity:entity"));
		spawningCondition = new ProcedureSelector(this.withEntry("entity/condition_natural_spawning"), mcreator,
				"Natural entity spawning condition", VariableElementType.LOGIC,
				Dependency.fromString("x:number/y:number/z:number/world:world"))
				.setDefaultName("(use vanilla condition)");

		restrictionBiomes = new BiomeListField(mcreator);
		breedTriggerItems = new MCItemListField(mcreator, ElementUtil::loadBlocksAndItems);

		mobModelTexture.setRenderer(new WTextureComboBoxRenderer.OtherTextures(mcreator.getWorkspace()));
		mobModelGlowTexture.setRenderer(new WTextureComboBoxRenderer.OtherTextures(mcreator.getWorkspace()));

		guiBoundTo.addActionListener(e -> {
			if (!isEditingMode()) {
				String selected = (String) guiBoundTo.getSelectedItem();
				if (selected != null) {
					ModElement element = mcreator.getWorkspace().getModElementByName(selected);
					if (element != null) {
						GeneratableElement generatableElement = element.getGeneratableElement();
						if (generatableElement instanceof GUI) {
							inventorySize.setValue(((GUI) generatableElement).getMaxSlotID() + 1);
						}
					}
				}
			}
		});

		spawnInDungeons.setOpaque(false);
		mobModelTexture.setPrototypeDisplayValue("XXXXXXXXXXXXXXXXXXXXXXXXXX");
		mobModelGlowTexture.setPrototypeDisplayValue("XXXXXXXXXXXXXXXXXXXXXXXXXX");

		mobDrop = new MCItemHolder(mcreator, ElementUtil::loadBlocksAndItems);
		equipmentMainHand = new MCItemHolder(mcreator, ElementUtil::loadBlocksAndItems);
		equipmentHelmet = new MCItemHolder(mcreator, ElementUtil::loadBlocksAndItems);
		equipmentBody = new MCItemHolder(mcreator, ElementUtil::loadBlocksAndItems);
		equipmentLeggings = new MCItemHolder(mcreator, ElementUtil::loadBlocksAndItems);
		equipmentBoots = new MCItemHolder(mcreator, ElementUtil::loadBlocksAndItems);
		equipmentOffHand = new MCItemHolder(mcreator, ElementUtil::loadBlocksAndItems);
		rangedAttackItem = new MCItemHolder(mcreator, ElementUtil::loadBlocksAndItems);

		JPanel pane1 = new JPanel(new BorderLayout(0, 0));
		JPanel pane2 = new JPanel(new BorderLayout(0, 0));
		JPanel pane3 = new JPanel(new BorderLayout(0, 0));
		JPanel pane4 = new JPanel(new BorderLayout(0, 0));
		JPanel pane6 = new JPanel(new BorderLayout(0, 0));
		JPanel pane5 = new JPanel(new BorderLayout(0, 0));
		JPanel pane7 = new JPanel(new BorderLayout(0, 0));

		JPanel subpane1 = new JPanel(new GridLayout(12, 2, 0, 2));

		immuneToFire.setOpaque(false);
		immuneToArrows.setOpaque(false);
		immuneToFallDamage.setOpaque(false);
		immuneToCactus.setOpaque(false);
		immuneToDrowning.setOpaque(false);
		immuneToLightning.setOpaque(false);
		immuneToPotions.setOpaque(false);
		immuneToPlayer.setOpaque(false);

		subpane1.setOpaque(false);

		subpane1.add(HelpUtils.wrapWithHelpButton(this.withEntry("entity/behaviour"),
				new JLabel("<html>Behavioural characteristics (Mob is aggressive, Creature is passive):")));
		subpane1.add(mobBehaviourType);

		subpane1.add(HelpUtils.wrapWithHelpButton(this.withEntry("entity/creature_type"),
				new JLabel("<html>Creature type (defines some special entity attributes):")));
		subpane1.add(mobCreatureType);

		subpane1.add(new JLabel("Entity health value, experience amount: "));
		subpane1.add(PanelUtils.join(HelpUtils.wrapWithHelpButton(this.withEntry("entity/health"), health),
				HelpUtils.wrapWithHelpButton(this.withEntry("entity/xp_amount"), xpAmount)));

		subpane1.add(HelpUtils.wrapWithHelpButton(this.withEntry("entity/drop"),
				new JLabel("<html>Mob default drop:<br><small>Drop is optional, leave blank for no drop")));
		subpane1.add(PanelUtils.join(mobDrop));

		subpane1.add(new JLabel("Movement speed, tracking range (\"render distance\"): "));
		subpane1.add(PanelUtils
				.join(HelpUtils.wrapWithHelpButton(this.withEntry("entity/movement_speed"), movementSpeed),
						HelpUtils.wrapWithHelpButton(this.withEntry("entity/tracking_range"), trackingRange)));

		subpane1.add(new JLabel("Attack strength, armor protection base value: "));
		subpane1.add(PanelUtils
				.join(HelpUtils.wrapWithHelpButton(this.withEntry("entity/attack_strength"), attackStrength),
						HelpUtils.wrapWithHelpButton(this.withEntry("entity/armor_base_value"), armorBaseValue)));

		subpane1.add(new JLabel("Attack knockback, knockback resistance: "));
		subpane1.add(PanelUtils
				.join(HelpUtils.wrapWithHelpButton(this.withEntry("entity/attack_knockback"), attackKnockback),
						HelpUtils.wrapWithHelpButton(this.withEntry("entity/knockback_resistance"),
								knockbackResistance)));

		subpane1.add(HelpUtils.wrapWithHelpButton(this.withEntry("entity/equipment"), new JLabel(
				"<html>Equipment (optional: main hand, off hand, helmet, body, leggings, boots):<br><small>Only works for Biped and Zombie models")));
		subpane1.add(PanelUtils
				.join(equipmentMainHand, equipmentOffHand, equipmentHelmet, equipmentBody, equipmentLeggings,
						equipmentBoots));

		subpane1.add(HelpUtils.wrapWithHelpButton(this.withEntry("entity/immunity"), new JLabel("Entity immune to: ")));
		subpane1.add(PanelUtils
				.gridElements(2, 4, 0, 0, immuneToFire, immuneToArrows, immuneToFallDamage, immuneToCactus,
						immuneToDrowning, immuneToLightning, immuneToPotions, immuneToPlayer));

		subpane1.add(HelpUtils.wrapWithHelpButton(this.withEntry("entity/ridable"), new JLabel(
				"<html>Check to make this entity rideable by player"
						+ "<br><small>You can optionally turn on living entity controls too")));
		subpane1.add(PanelUtils.join(FlowLayout.LEFT, ridable, canControlForward, canControlStrafe));

		subpane1.add(HelpUtils.wrapWithHelpButton(this.withEntry("entity/water_entity"), new JLabel(
				"<html>Check if this entity is water entity:"
						+ "<br><small>NOTE: You still need AI tasks to make mob use these properties")));
		subpane1.add(waterMob);

		subpane1.add(HelpUtils.wrapWithHelpButton(this.withEntry("entity/flying_entity"), new JLabel(
				"<html>Check if this entity is flying entity:"
						+ "<br><small>NOTE: You still need AI tasks to make mob use these properties")));
		subpane1.add(flyingMob);

		hasAI.setOpaque(false);
		isBoss.setOpaque(false);
		waterMob.setOpaque(false);
		flyingMob.setOpaque(false);
		hasSpawnEgg.setOpaque(false);
		disableCollisions.setOpaque(false);

		livingSound.setText("");
		hurtSound.setText("entity.generic.hurt");
		deathSound.setText("entity.generic.death");

		pane1.add("Center", PanelUtils.totalCenterInPanel(subpane1));

		JPanel spo2 = new JPanel(new GridLayout(12, 2, 0, 2));

		spo2.setOpaque(false);

		spo2.add(HelpUtils.wrapWithHelpButton(this.withEntry("entity/name"), new JLabel("Name of entity: ")));
		spo2.add(mobName);

		spo2.add(HelpUtils.wrapWithHelpButton(this.withEntry("entity/model"),
				new JLabel("<html>Entity model:<br><small>Supported: JAVA")));
		spo2.add(mobModel);

		JButton importmobtexture = new JButton(UIRES.get("18px.add"));
		importmobtexture.setToolTipText("Click this to import entity model texture");
		importmobtexture.setOpaque(false);
		importmobtexture.addActionListener(e -> {
			TextureImportDialogs.importOtherTextures(mcreator);
			mobModelTexture.removeAllItems();
			mobModelTexture.addItem("");
			mcreator.getWorkspace().getFolderManager().getOtherTexturesList()
					.forEach(el -> mobModelTexture.addItem(el.getName()));
			mobModelGlowTexture.removeAllItems();
			mobModelGlowTexture.addItem("");
			mcreator.getWorkspace().getFolderManager().getOtherTexturesList()
					.forEach(el -> mobModelGlowTexture.addItem(el.getName()));
		});

		spo2.add(HelpUtils.wrapWithHelpButton(this.withEntry("entity/texture"), new JLabel(
				"<html>Texture of entity:<br><small>"
						+ "Texture needs to fit to the model (e.g. player skin texture is NOT biped model texture)")));
		spo2.add(PanelUtils.centerAndEastElement(mobModelTexture, importmobtexture));

		spo2.add(HelpUtils.wrapWithHelpButton(this.withEntry("entity/glow_texture"), new JLabel(
				"<html>Glow texture of entity (optional):<br><small>"
						+ "Glow texture needs to fit to the model, image brightness determines glow")));
		spo2.add(mobModelGlowTexture);

		ComponentUtils.deriveFont(mobModelTexture, 16);
		ComponentUtils.deriveFont(mobModelGlowTexture, 16);
		ComponentUtils.deriveFont(aiBase, 16);
		ComponentUtils.deriveFont(mobModel, 16);
		ComponentUtils.deriveFont(rangedItemType, 16);

		mobModel.setRenderer(new ModelComboBoxRenderer());

		spawnEggBaseColor.setOpaque(false);
		spawnEggDotColor.setOpaque(false);

		modelWidth.setPreferredSize(new Dimension(85, 32));
		mountedYOffset.setPreferredSize(new Dimension(85, 32));
		modelHeight.setPreferredSize(new Dimension(85, 32));
		modelShadowSize.setPreferredSize(new Dimension(85, 32));

		armorBaseValue.setPreferredSize(new Dimension(250, 32));

		movementSpeed.setPreferredSize(new Dimension(250, 32));
		trackingRange.setPreferredSize(new Dimension(250, 32));
		attackStrength.setPreferredSize(new Dimension(250, 32));
		attackKnockback.setPreferredSize(new Dimension(250, 32));
		knockbackResistance.setPreferredSize(new Dimension(250, 32));

		health.setPreferredSize(new Dimension(250, 32));
		xpAmount.setPreferredSize(new Dimension(250, 32));

		mobModel.addActionListener(e -> {
			if (disableMobModelCheckBoxListener)
				return;

			if (biped.equals(mobModel.getSelectedItem())) {
				modelWidth.setValue(0.6);
				modelHeight.setValue(1.8);
			} else if (chicken.equals(mobModel.getSelectedItem())) {
				modelWidth.setValue(0.4);
				modelHeight.setValue(0.7);
			} else if (cow.equals(mobModel.getSelectedItem())) {
				modelWidth.setValue(0.9);
				modelHeight.setValue(1.4);
			} else if (creeper.equals(mobModel.getSelectedItem())) {
				modelWidth.setValue(0.6);
				modelHeight.setValue(1.7);
			} else if (ghast.equals(mobModel.getSelectedItem())) {
				modelWidth.setValue(1.0);
				modelHeight.setValue(1.0);
			} else if (pig.equals(mobModel.getSelectedItem())) {
				modelWidth.setValue(0.9);
				modelHeight.setValue(0.9);
			} else if (slime.equals(mobModel.getSelectedItem())) {
				modelWidth.setValue(1.0);
				modelHeight.setValue(1.0);
			} else if (spider.equals(mobModel.getSelectedItem())) {
				modelWidth.setValue(1.4);
				modelHeight.setValue(0.9);
			} else if (villager.equals(mobModel.getSelectedItem())) {
				modelWidth.setValue(0.6);
				modelHeight.setValue(1.95);
			} else if (silverfish.equals(mobModel.getSelectedItem())) {
				modelWidth.setValue(0.4);
				modelHeight.setValue(0.3);
			}
		});

		spo2.add(HelpUtils.wrapWithHelpButton(this.withEntry("entity/bounding_box"), new JLabel(
				"<html>Entity model bounding box:<br><small>Width/Depth, Height, Shadow Size, Mounted Entity Y Offset")));
		spo2.add(PanelUtils.join(FlowLayout.LEFT, modelWidth, modelHeight, new JEmptyBox(7, 7), modelShadowSize,
				new JEmptyBox(7, 7), mountedYOffset, new JEmptyBox(7, 7), disableCollisions));

		spo2.add(HelpUtils.wrapWithHelpButton(this.withEntry("entity/spawn_egg_options"),
				new JLabel("<html>Spawn egg options:<br><small>Enable, base color, dot color, creative tab")));
		spo2.add(PanelUtils.join(hasSpawnEgg, spawnEggBaseColor, spawnEggDotColor, creativeTab));

		bossBarColor.setEnabled(false);
		bossBarType.setEnabled(false);

		spo2.add(HelpUtils.wrapWithHelpButton(this.withEntry("entity/boss_entity"),
				new JLabel("Select if you want your entity to be boss with health bar: ")));
		spo2.add(PanelUtils.join(isBoss, bossBarColor, bossBarType));

		spo2.add(HelpUtils.wrapWithHelpButton(this.withEntry("entity/label"),
				new JLabel("Entity label (leave blank to hide it):")));
		spo2.add(mobLabel);

		spo2.add(HelpUtils.wrapWithHelpButton(this.withEntry("entity/sound"), new JLabel("Living sound: ")));
		spo2.add(livingSound);

		spo2.add(HelpUtils.wrapWithHelpButton(this.withEntry("entity/step_sound"),
				new JLabel("<html>Step sound:<br><small>" + "Leave empty for step sounds from blocks")));
		spo2.add(stepSound);

		spo2.add(HelpUtils.wrapWithHelpButton(this.withEntry("entity/hurt_sound"), new JLabel("Hurt sound: ")));
		spo2.add(hurtSound);

		spo2.add(HelpUtils.wrapWithHelpButton(this.withEntry("entity/death_sound"), new JLabel("Death sound: ")));
		spo2.add(deathSound);

		ComponentUtils.deriveFont(mobLabel, 16);

		pane2.setOpaque(false);

		pane2.add("Center", PanelUtils.totalCenterInPanel(spo2));

		JPanel aitop = new JPanel(new GridLayout(2, 2, 10, 10));
		aitop.setOpaque(false);
		aitop.add(PanelUtils
				.join(FlowLayout.LEFT, HelpUtils.wrapWithHelpButton(this.withEntry("entity/enable_ai"), hasAI)));

		aitop.add(PanelUtils.join(FlowLayout.LEFT, new JEmptyBox(20, 5), HelpUtils
				.wrapWithHelpButton(this.withEntry("entity/base"), new JLabel(
						"<html><b>Entity base:</b>" + "<br><small>This option will override some of the parameters!"
								+ "<br>Using entity base can introduce problems so avoid using it")), aiBase));

		aitop.add(PanelUtils
				.join(FlowLayout.LEFT, HelpUtils.wrapWithHelpButton(this.withEntry("entity/breedable"), breedable),
						breedTriggerItems, tameable));

		breedTriggerItems.setPreferredSize(new Dimension(300, 32));
		aiBase.setPreferredSize(new Dimension(250, 32));

		aitop.add(PanelUtils
				.join(FlowLayout.LEFT, HelpUtils.wrapWithHelpButton(this.withEntry("entity/do_ranged_attacks"), ranged),
						new JLabel(" with "), rangedItemType, rangedAttackItem));

		rangedAttackItem.setEnabled(false);

		rangedItemType.addActionListener(
				e -> rangedAttackItem.setEnabled("Default item".equals(rangedItemType.getSelectedItem())));

		ridable.setOpaque(false);
		canControlStrafe.setOpaque(false);
		canControlForward.setOpaque(false);

		JPanel aitopwrp = PanelUtils.maxMargin(aitop, 2, true, true, true, true);
		aitopwrp.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder((Color) UIManager.get("MCreatorLAF.BRIGHT_COLOR"), 1), "AI parameters",
				0, 0, getFont().deriveFont(12.0f), (Color) UIManager.get("MCreatorLAF.BRIGHT_COLOR")));

		JPanel aipan = new JPanel(new BorderLayout(0, 5));
		aipan.setOpaque(false);

		externalBlocks = BlocklyLoader.INSTANCE.getAITaskBlockLoader().getDefinedBlocks();

		blocklyPanel = new BlocklyPanel(mcreator);
		blocklyPanel.addTaskToRunAfterLoaded(() -> {
			BlocklyLoader.INSTANCE.getAITaskBlockLoader()
					.loadBlocksAndCategoriesInPanel(blocklyPanel, ExternalBlockLoader.ToolboxType.EMPTY);
			blocklyPanel.getJSBridge()
					.setJavaScriptEventListener(() -> new Thread(LivingEntityGUI.this::regenerateAITasks).start());
			if (!isEditingMode()) {
				setDefaultAISet();
			}
		});

		aipan.add("North", aitopwrp);

		JPanel bpb = new JPanel(new GridLayout());
		bpb.setOpaque(false);
		bpb.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder((Color) UIManager.get("MCreatorLAF.BRIGHT_COLOR"), 1),
				"Living entity AI tasks / goals builder", TitledBorder.LEADING, TitledBorder.DEFAULT_POSITION,
				getFont(), Color.white));
		bpb.add(PanelUtils.northAndCenterElement(new AITasksEditorToolbar(mcreator, blocklyPanel), blocklyPanel));
		aipan.add("Center", bpb);
		aipan.add("South", compileNotesPanel);

		blocklyPanel.setPreferredSize(new Dimension(150, 150));

		pane3.add("Center", PanelUtils.maxMargin(aipan, 10, true, true, true, true));

		breedable.setOpaque(false);
		tameable.setOpaque(false);
		ranged.setOpaque(false);

		hasAI.setSelected(true);

		breedable.addActionListener(actionEvent -> {
			if (breedable.isSelected()) {
				hasAI.setSelected(true);
				hasAI.setEnabled(false);
				this.breedTriggerItems.setEnabled(true);
				this.tameable.setEnabled(true);
			} else {
				hasAI.setEnabled(true);
				this.breedTriggerItems.setEnabled(false);
				this.tameable.setEnabled(false);
			}
		});

		isBoss.addActionListener(e -> {
			bossBarColor.setEnabled(isBoss.isSelected());
			bossBarType.setEnabled(isBoss.isSelected());
		});

		pane3.setOpaque(false);

		JPanel events = new JPanel(new GridLayout(3, 4, 8, 8));
		events.add(onStruckByLightning);
		events.add(whenMobFalls);
		events.add(whenMobDies);
		events.add(whenMobIsHurt);
		events.add(onRightClickedOn);
		events.add(whenThisMobKillsAnother);
		events.add(onMobTickUpdate);
		events.add(onPlayerCollidesWith);
		events.add(onInitialSpawn);
		events.setOpaque(false);
		pane4.add("Center", PanelUtils.totalCenterInPanel(events));

		JPanel particles = new JPanel(new BorderLayout());
		particles.setOpaque(false);

		spawnParticles.setOpaque(false);

		JPanel options = new JPanel(new GridLayout(5, 2, 0, 2));

		options.add(HelpUtils.wrapWithHelpButton(this.withEntry("particle/gen_particles"), spawnParticles));
		options.add(new JLabel());

		options.add(HelpUtils.wrapWithHelpButton(this.withEntry("particle/gen_type"), new JLabel("Particle type: ")));
		options.add(particleToSpawn);

		options.add(HelpUtils.wrapWithHelpButton(this.withEntry("particle/gen_shape"),
				new JLabel("Particle spawning shape (determines shape boundary): ")));
		options.add(particleSpawningShape);

		options.add(HelpUtils.wrapWithHelpButton(this.withEntry("particle/gen_spawn_radius"),
				new JLabel("Average particle spawning radius: ")));
		options.add(particleSpawningRadious);

		options.add(HelpUtils.wrapWithHelpButton(this.withEntry("particle/gen_average_amount"),
				new JLabel("Average particle amount: ")));
		options.add(particleAmount);

		options.setOpaque(false);

		isBoss.setOpaque(false);

		particles.add("West", PanelUtils
				.join(PanelUtils.northAndCenterElement(options, PanelUtils.join(FlowLayout.LEFT, particleCondition))));
		pane6.add("Center", PanelUtils.totalCenterInPanel(particles));

		pane4.setOpaque(false);

		JPanel selp = new JPanel(new GridLayout(8, 2, 30, 2));

		ComponentUtils.deriveFont(mobName, 16);

		spawnThisMob.setSelected(true);
		doesDespawnWhenIdle.setSelected(true);

		spawnThisMob.setOpaque(false);
		doesDespawnWhenIdle.setOpaque(false);

		hasSpawnEgg.setSelected(true);

		selp.add(HelpUtils
				.wrapWithHelpButton(this.withEntry("entity/enable_spawning"), new JLabel("Enable entity spawning: ")));
		selp.add(spawnThisMob);

		selp.add(
				HelpUtils.wrapWithHelpButton(this.withEntry("entity/despawn_idle"), new JLabel("Despawn when idle: ")));
		selp.add(doesDespawnWhenIdle);

		selp.add(HelpUtils.wrapWithHelpButton(this.withEntry("entity/spawn_weight"), new JLabel("Spawn weight: ")));
		selp.add(spawningProbability);

		selp.add(HelpUtils.wrapWithHelpButton(this.withEntry("entity/spawn_type"),
				new JLabel("<html>Mob natural spawning type (affects the spawning location and time range):")));
		selp.add(mobSpawningType);

		selp.add(HelpUtils.wrapWithHelpButton(this.withEntry("entity/spawn_group_size"),
				new JLabel("Minimal number of entities in a spawn group: ")));
		selp.add(minNumberOfMobsPerGroup);

		selp.add(HelpUtils.wrapWithHelpButton(this.withEntry("entity/spawn_group_size"),
				new JLabel("Maximal number of entities in a spawn group: ")));
		selp.add(maxNumberOfMobsPerGroup);

		selp.add(HelpUtils.wrapWithHelpButton(this.withEntry("common/restrict_to_biomes"), new JLabel(
				"<html>Biomes where the entity spawns (empty for no restriction):<br><small>"
						+ "For custom biomes, this can be defined in biome editor too")));
		selp.add(restrictionBiomes);

		selp.add(HelpUtils.wrapWithHelpButton(this.withEntry("entity/spawn_in_dungeons"),
				new JLabel("Does this entity spawn in dungeons? ")));
		selp.add(spawnInDungeons);

		selp.setOpaque(false);

		JComponent selpcont = PanelUtils.northAndCenterElement(selp, PanelUtils.westAndEastElement(new JLabel(
						"<html>Custom spawning condition:<br>"
								+ "<small>When custom spawning condition is used, spawning type condition will be overwritten<br>"
								+ "with it, but spawning type grouping and weighting will still be used.<br>"),
				PanelUtils.join(spawningCondition)));

		pane5.add("Center", PanelUtils.totalCenterInPanel(selpcont));

		pane5.setOpaque(false);

		JPanel props = new JPanel(new GridLayout(3, 2, 35, 2));
		props.setOpaque(false);

		props.add(HelpUtils.wrapWithHelpButton(this.withEntry("entity/bind_gui"), new JLabel(
				"<html>Bind this entity to GUI:<br>"
						+ "<small>Set to Empty to disable inventory (you want this in most cases)<br>")));
		props.add(guiBoundTo);

		props.add(HelpUtils.wrapWithHelpButton(this.withEntry("entity/inventory_size"), new JLabel(
				"<html>Size of inventory (slot count):<br><small>"
						+ "Set this value to the <i>biggest slot ID in the GUI</i> + 1")));
		props.add(inventorySize);

		props.add(HelpUtils
				.wrapWithHelpButton(this.withEntry("entity/inventory_stack_size"), new JLabel("Max size of stack:")));
		props.add(inventoryStackSize);

		pane7.add(PanelUtils.totalCenterInPanel(props));
		pane7.setOpaque(false);
		pane7.setOpaque(false);

		mobModelTexture.setValidator(() -> {
			if (mobModelTexture.getSelectedItem() == null || mobModelTexture.getSelectedItem().equals(""))
				return new Validator.ValidationResult(Validator.ValidationResultType.ERROR,
						"Entity model needs to have a texture");
			return new Validator.ValidationResult(Validator.ValidationResultType.PASSED, "");
		});

		mobName.setValidator(new TextFieldValidator(mobName, "Entity needs a name"));
		mobName.enableRealtimeValidation();

		pane1.setOpaque(false);
		pane6.setOpaque(false);

		addPage("Visual and sound", pane2);
		addPage("Behaviour", pane1);
		addPage("Particles", pane6);
		addPage("Inventory", pane7);
		addPage("Triggers", pane4);
		addPage("AI and goals", pane3);
		addPage("Spawning", pane5);

		if (!isEditingMode()) {
			String readableNameFromModElement = StringUtils.machineToReadableName(modElement.getName());
			mobName.setText(readableNameFromModElement);
		}
	}

	@Override public void reloadDataLists() {
		disableMobModelCheckBoxListener = true;

		super.reloadDataLists();
		onStruckByLightning.refreshListKeepSelected();
		whenMobFalls.refreshListKeepSelected();
		whenMobDies.refreshListKeepSelected();
		whenMobIsHurt.refreshListKeepSelected();
		onRightClickedOn.refreshListKeepSelected();
		whenThisMobKillsAnother.refreshListKeepSelected();
		onMobTickUpdate.refreshListKeepSelected();
		onPlayerCollidesWith.refreshListKeepSelected();
		onInitialSpawn.refreshListKeepSelected();

		particleCondition.refreshListKeepSelected();
		spawningCondition.refreshListKeepSelected();

		ComboBoxUtil.updateComboBoxContents(mobModelTexture, ListUtils.merge(Collections.singleton(""),
				mcreator.getWorkspace().getFolderManager().getOtherTexturesList().stream().map(File::getName)
						.collect(Collectors.toList())), "");

		ComboBoxUtil.updateComboBoxContents(mobModelGlowTexture, ListUtils.merge(Collections.singleton(""),
				mcreator.getWorkspace().getFolderManager().getOtherTexturesList().stream().map(File::getName)
						.collect(Collectors.toList())), "");

		ComboBoxUtil.updateComboBoxContents(mobModel, ListUtils.merge(Arrays.asList(builtinmobmodels),
				Model.getModelsWithTextureMaps(mcreator.getWorkspace()).stream()
						.filter(el -> el.getType() == Model.Type.JAVA || el.getType() == Model.Type.MCREATOR)
						.collect(Collectors.toList())));

		ComboBoxUtil.updateComboBoxContents(creativeTab, ElementUtil.loadAllTabs(mcreator.getWorkspace()),
				new DataListEntry.Dummy("MISC"));

		ComboBoxUtil.updateComboBoxContents(rangedItemType, ListUtils.merge(Collections.singleton("Default item"),
				mcreator.getWorkspace().getModElements().stream()
						.filter(var -> var.getType() == ModElementType.RANGEDITEM).map(ModElement::getName)
						.collect(Collectors.toList())), "Default item");

		ComboBoxUtil.updateComboBoxContents(guiBoundTo, ListUtils.merge(Collections.singleton("<NONE>"),
				mcreator.getWorkspace().getModElements().stream().filter(var -> var.getType() == ModElementType.GUI)
						.map(ModElement::getName).collect(Collectors.toList())), "<NONE>");

		disableMobModelCheckBoxListener = false;
	}

	@Override protected AggregatedValidationResult validatePage(int page) {
		if (page == 0) {
			return new AggregatedValidationResult(mobModelTexture, mobName);
		} else if (page == 5) {
			if (hasErrors)
				return new AggregatedValidationResult.MULTIFAIL(compileNotesPanel.getCompileNotes().stream()
						.map(compileNote -> "Mob AI builder: " + compileNote.getMessage())
						.collect(Collectors.toList()));
		} else if (page == 6) {
			if ((int) minNumberOfMobsPerGroup.getValue() > (int) maxNumberOfMobsPerGroup.getValue()) {
				return new AggregatedValidationResult.FAIL("Minimal mob group size can't be bigger than maximal size");
			}
		}
		return new AggregatedValidationResult.PASS();
	}

	@Override public void openInEditingMode(Mob mob) {
		disableMobModelCheckBoxListener = true;
		mobName.setText(mob.mobName);
		mobModelTexture.setSelectedItem(mob.mobModelTexture);
		mobModelGlowTexture.setSelectedItem(mob.mobModelGlowTexture);
		mobSpawningType.setSelectedItem(mob.mobSpawningType);
		rangedItemType.setSelectedItem(mob.rangedItemType);
		spawnEggBaseColor.setColor(mob.spawnEggBaseColor);
		spawnEggDotColor.setColor(mob.spawnEggDotColor);
		mobLabel.setText(mob.mobLabel);
		onStruckByLightning.setSelectedProcedure(mob.onStruckByLightning);
		whenMobFalls.setSelectedProcedure(mob.whenMobFalls);
		whenMobDies.setSelectedProcedure(mob.whenMobDies);
		whenMobIsHurt.setSelectedProcedure(mob.whenMobIsHurt);
		onRightClickedOn.setSelectedProcedure(mob.onRightClickedOn);
		whenThisMobKillsAnother.setSelectedProcedure(mob.whenThisMobKillsAnother);
		onMobTickUpdate.setSelectedProcedure(mob.onMobTickUpdate);
		onPlayerCollidesWith.setSelectedProcedure(mob.onPlayerCollidesWith);
		onInitialSpawn.setSelectedProcedure(mob.onInitialSpawn);
		mobBehaviourType.setSelectedItem(mob.mobBehaviourType);
		mobCreatureType.setSelectedItem(mob.mobCreatureType);
		attackStrength.setValue(mob.attackStrength);
		attackKnockback.setValue(mob.attackKnockback);
		knockbackResistance.setValue(mob.knockbackResistance);
		movementSpeed.setValue(mob.movementSpeed);
		mobDrop.setBlock(mob.mobDrop);
		equipmentMainHand.setBlock(mob.equipmentMainHand);
		equipmentHelmet.setBlock(mob.equipmentHelmet);
		equipmentBody.setBlock(mob.equipmentBody);
		equipmentLeggings.setBlock(mob.equipmentLeggings);
		equipmentBoots.setBlock(mob.equipmentBoots);
		health.setValue(mob.health);
		trackingRange.setValue(mob.trackingRange);
		immuneToFire.setSelected(mob.immuneToFire);
		immuneToArrows.setSelected(mob.immuneToArrows);
		immuneToFallDamage.setSelected(mob.immuneToFallDamage);
		immuneToCactus.setSelected(mob.immuneToCactus);
		immuneToDrowning.setSelected(mob.immuneToDrowning);
		immuneToLightning.setSelected(mob.immuneToLightning);
		immuneToPotions.setSelected(mob.immuneToPotions);
		immuneToPlayer.setSelected(mob.immuneToPlayer);
		xpAmount.setValue(mob.xpAmount);
		livingSound.setSound(mob.livingSound);
		hurtSound.setSound(mob.hurtSound);
		deathSound.setSound(mob.deathSound);
		stepSound.setSound(mob.stepSound);
		hasAI.setSelected(mob.hasAI);
		isBoss.setSelected(mob.isBoss);
		hasSpawnEgg.setSelected(mob.hasSpawnEgg);
		disableCollisions.setSelected(mob.disableCollisions);
		aiBase.setSelectedItem(mob.aiBase);
		spawningProbability.setValue(mob.spawningProbability);
		minNumberOfMobsPerGroup.setValue(mob.minNumberOfMobsPerGroup);
		maxNumberOfMobsPerGroup.setValue(mob.maxNumberOfMobsPerGroup);
		spawnInDungeons.setSelected(mob.spawnInDungeons);
		restrictionBiomes.setListElements(mob.restrictionBiomes);
		spawnParticles.setSelected(mob.spawnParticles);
		particleToSpawn.setSelectedItem(mob.particleToSpawn.getUnmappedValue());
		particleSpawningShape.setSelectedItem(mob.particleSpawningShape);
		particleCondition.setSelectedProcedure(mob.particleCondition);
		spawningCondition.setSelectedProcedure(mob.spawningCondition);
		particleSpawningRadious.setValue(mob.particleSpawningRadious);
		particleAmount.setValue(mob.particleAmount);
		breedTriggerItems.setListElements(mob.breedTriggerItems);
		bossBarColor.setSelectedItem(mob.bossBarColor);
		bossBarType.setSelectedItem(mob.bossBarType);
		equipmentOffHand.setBlock(mob.equipmentOffHand);
		ridable.setSelected(mob.ridable);
		canControlStrafe.setSelected(mob.canControlStrafe);
		canControlForward.setSelected(mob.canControlForward);
		breedable.setSelected(mob.breedable);
		tameable.setSelected(mob.tameable);
		ranged.setSelected(mob.ranged);
		rangedAttackItem.setBlock(mob.rangedAttackItem);
		spawnThisMob.setSelected(mob.spawnThisMob);
		doesDespawnWhenIdle.setSelected(mob.doesDespawnWhenIdle);
		modelWidth.setValue(mob.modelWidth);
		modelHeight.setValue(mob.modelHeight);
		mountedYOffset.setValue(mob.mountedYOffset);
		modelShadowSize.setValue(mob.modelShadowSize);
		armorBaseValue.setValue(mob.armorBaseValue);
		waterMob.setSelected(mob.waterMob);
		flyingMob.setSelected(mob.flyingMob);
		guiBoundTo.setSelectedItem(mob.guiBoundTo);
		inventorySize.setValue(mob.inventorySize);
		inventoryStackSize.setValue(mob.inventoryStackSize);

		if (mob.creativeTab != null)
			creativeTab.setSelectedItem(mob.creativeTab);

		Model model = mob.getEntityModel();
		if (model != null && model.getType() != null && model.getReadableName() != null)
			mobModel.setSelectedItem(model);

		blocklyPanel.setXMLDataOnly(mob.aixml);
		blocklyPanel.addTaskToRunAfterLoaded(() -> {
			blocklyPanel.clearWorkspace();
			blocklyPanel.setXML(mob.aixml);
			regenerateAITasks();
		});

		if (breedable.isSelected()) {
			hasAI.setSelected(true);
			hasAI.setEnabled(false);
			this.breedTriggerItems.setEnabled(true);
			this.tameable.setEnabled(true);
		} else {
			hasAI.setEnabled(true);
			this.breedTriggerItems.setEnabled(false);
			this.tameable.setEnabled(false);
		}

		bossBarColor.setEnabled(isBoss.isSelected());
		bossBarType.setEnabled(isBoss.isSelected());

		rangedAttackItem.setEnabled("Default item".equals(rangedItemType.getSelectedItem()));

		disableMobModelCheckBoxListener = false;
	}

	@Override public Mob getElementFromGUI() {
		Mob mob = new Mob(modElement);
		mob.mobName = mobName.getText();
		mob.mobLabel = mobLabel.getText();
		mob.mobModelTexture = mobModelTexture.getSelectedItem();
		mob.mobModelGlowTexture = mobModelGlowTexture.getSelectedItem();
		mob.spawnEggBaseColor = spawnEggBaseColor.getColor();
		mob.spawnEggDotColor = spawnEggDotColor.getColor();
		mob.hasSpawnEgg = hasSpawnEgg.isSelected();
		mob.disableCollisions = disableCollisions.isSelected();
		mob.isBoss = isBoss.isSelected();
		mob.bossBarColor = (String) bossBarColor.getSelectedItem();
		mob.bossBarType = (String) bossBarType.getSelectedItem();
		mob.equipmentMainHand = equipmentMainHand.getBlock();
		mob.equipmentOffHand = equipmentOffHand.getBlock();
		mob.equipmentHelmet = equipmentHelmet.getBlock();
		mob.equipmentBody = equipmentBody.getBlock();
		mob.equipmentLeggings = equipmentLeggings.getBlock();
		mob.equipmentBoots = equipmentBoots.getBlock();
		mob.mobBehaviourType = (String) mobBehaviourType.getSelectedItem();
		mob.mobCreatureType = (String) mobCreatureType.getSelectedItem();
		mob.attackStrength = (int) attackStrength.getValue();
		mob.attackKnockback = (double) attackKnockback.getValue();
		mob.knockbackResistance = (double) knockbackResistance.getValue();
		mob.movementSpeed = (double) movementSpeed.getValue();
		mob.health = (int) health.getValue();
		mob.trackingRange = (int) trackingRange.getValue();
		mob.immuneToFire = immuneToFire.isSelected();
		mob.immuneToArrows = immuneToArrows.isSelected();
		mob.immuneToFallDamage = immuneToFallDamage.isSelected();
		mob.immuneToCactus = immuneToCactus.isSelected();
		mob.immuneToDrowning = immuneToDrowning.isSelected();
		mob.immuneToLightning = immuneToLightning.isSelected();
		mob.immuneToPotions = immuneToPotions.isSelected();
		mob.immuneToPlayer = immuneToPlayer.isSelected();
		mob.xpAmount = (int) xpAmount.getValue();
		mob.ridable = ridable.isSelected();
		mob.canControlForward = canControlForward.isSelected();
		mob.canControlStrafe = canControlStrafe.isSelected();
		mob.mobDrop = mobDrop.getBlock();
		mob.livingSound = livingSound.getSound();
		mob.hurtSound = hurtSound.getSound();
		mob.deathSound = deathSound.getSound();
		mob.stepSound = stepSound.getSound();
		mob.spawnParticles = spawnParticles.isSelected();
		mob.particleToSpawn = new Particle(mcreator.getWorkspace(), (String) particleToSpawn.getSelectedItem());
		mob.particleSpawningShape = (String) particleSpawningShape.getSelectedItem();
		mob.particleSpawningRadious = (double) particleSpawningRadious.getValue();
		mob.particleAmount = (int) particleAmount.getValue();
		mob.particleCondition = particleCondition.getSelectedProcedure();
		mob.spawningCondition = spawningCondition.getSelectedProcedure();
		mob.onStruckByLightning = onStruckByLightning.getSelectedProcedure();
		mob.whenMobFalls = whenMobFalls.getSelectedProcedure();
		mob.whenMobDies = whenMobDies.getSelectedProcedure();
		mob.whenMobIsHurt = whenMobIsHurt.getSelectedProcedure();
		mob.onRightClickedOn = onRightClickedOn.getSelectedProcedure();
		mob.whenThisMobKillsAnother = whenThisMobKillsAnother.getSelectedProcedure();
		mob.onMobTickUpdate = onMobTickUpdate.getSelectedProcedure();
		mob.onPlayerCollidesWith = onPlayerCollidesWith.getSelectedProcedure();
		mob.onInitialSpawn = onInitialSpawn.getSelectedProcedure();
		mob.hasAI = hasAI.isSelected();
		mob.aiBase = (String) aiBase.getSelectedItem();
		mob.aixml = blocklyPanel.getXML();
		mob.breedable = breedable.isSelected();
		mob.tameable = tameable.isSelected();
		mob.breedTriggerItems = breedTriggerItems.getListElements();
		mob.ranged = ranged.isSelected();
		mob.rangedAttackItem = rangedAttackItem.getBlock();
		mob.spawnThisMob = spawnThisMob.isSelected();
		mob.doesDespawnWhenIdle = doesDespawnWhenIdle.isSelected();
		mob.spawningProbability = (int) spawningProbability.getValue();
		mob.mobSpawningType = (String) mobSpawningType.getSelectedItem();
		mob.rangedItemType = (String) rangedItemType.getSelectedItem();
		mob.minNumberOfMobsPerGroup = (int) minNumberOfMobsPerGroup.getValue();
		mob.maxNumberOfMobsPerGroup = (int) maxNumberOfMobsPerGroup.getValue();
		mob.restrictionBiomes = restrictionBiomes.getListElements();
		mob.spawnInDungeons = spawnInDungeons.isSelected();
		mob.modelWidth = (double) modelWidth.getValue();
		mob.modelHeight = (double) modelHeight.getValue();
		mob.mountedYOffset = (double) mountedYOffset.getValue();
		mob.modelShadowSize = (double) modelShadowSize.getValue();
		mob.armorBaseValue = (double) armorBaseValue.getValue();
		mob.mobModelName = ((Model) Objects.requireNonNull(mobModel.getSelectedItem())).getReadableName();
		mob.waterMob = waterMob.isSelected();
		mob.flyingMob = flyingMob.isSelected();
		mob.creativeTab = new TabEntry(mcreator.getWorkspace(), creativeTab.getSelectedItem());
		mob.inventorySize = (int) inventorySize.getValue();
		mob.inventoryStackSize = (int) inventoryStackSize.getValue();
		mob.guiBoundTo = (String) guiBoundTo.getSelectedItem();
		return mob;
	}

	@Override public @Nullable URI getContextURL() throws URISyntaxException {
		return new URI(MCreatorApplication.SERVER_DOMAIN + "/wiki/how-make-mob");
	}

}