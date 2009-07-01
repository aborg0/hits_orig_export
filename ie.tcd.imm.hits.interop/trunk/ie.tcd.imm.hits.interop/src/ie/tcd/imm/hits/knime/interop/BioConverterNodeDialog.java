package ie.tcd.imm.hits.knime.interop;

import ie.tcd.imm.hits.knime.interop.BioConverterNodeModel.ConversionDefault;
import ie.tcd.imm.hits.knime.interop.config.Profile;
import ie.tcd.imm.hits.knime.interop.config.Value;
import ie.tcd.imm.hits.util.Displayable;
import ie.tcd.imm.hits.util.Pair;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import java.lang.reflect.Array;

import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentWithDefaults;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "BioConverter" Node. Converts between
 * different kind of plate formats.
 * <p>
 * The defaults, and profiles are read from {@code interop.xml}.
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more
 * complex dialog please derive directly from
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author <a href="bakosg@tcd.ie">Gabor Bakos</a>
 * @see BioConverterNodeModel
 * @see BioConverterNodeModel#root
 */
public class BioConverterNodeDialog extends DefaultNodeSettingsPane {

	/**
	 * Type of possible dialogs. For inputs there is no need of {@link #type}.
	 */
	public static enum DialogType {
		/** Group of {@link #name}, {@link #format}, and possibly {@link #type}. */
		group,
		/** Name of the column. */
		name,
		/** The format describing the input value structure, or the output. */
		format,
		/** Type of the output column. */
		type;

		private static final DialogType[] nonGroups = new DialogType[] { name,
				format, type };

		private static final DialogType[] inNonGroups = new DialogType[] {
				name, format };

		/**
		 * @param input
		 *            Selects the input ({@code true}) or output ({@code false})
		 *            group of {@link DialogType}s.
		 * @return The non-{@link #group} values for input or output columns.
		 */
		public static DialogType[] nonGroups(final boolean input) {
			return input ? inNonGroups : nonGroups;
		}

		/**
		 * @param value
		 *            The value to marshal.
		 * @return The name to use in marshaled xmls.
		 */
		public static String nameOf(final DialogType value) {
			return value.name();
		}
	}

	/**
	 * The handled column types.
	 */
	public static enum ColumnType implements Displayable {
		/** The plate information */
		Plate("Plate"),
		/** The replicate information */
		Replicate("Replicate");
		private final String displayText;

		private ColumnType(final String displayText) {
			this.displayText = displayText;
		}

		@Override
		public String getDisplayText() {
			return displayText;
		}

		/**
		 * @param value
		 *            The value to marshal.
		 * @return The name to use in marshaled xmls.
		 */
		public static String nameOf(final ColumnType value) {
			return value.name();
		}

	}

	private static final Map<String, Map<ColumnType, Map<Boolean, Map<DialogType, String>>>> patterns = new TreeMap</* ConversionDefault */String, Map<ColumnType, Map<Boolean, Map<DialogType, String>>>>();
	static {
		final List<Profile> profiles = BioConverterNodeModel.root.getProfiles()
				.getProfile();
		for (final Profile profile : profiles) {
			if (!patterns.containsKey(profile.getName())) {
				patterns.put(profile.getName(), createOuterEmptyMap());
			}
			if (profile.getExtends() != null) {
				final Map<ColumnType, Map<Boolean, Map<DialogType, String>>> ancestor = patterns
						.get(profile.getExtends());
				for (final Pair<Pair<ColumnType, Boolean>, DialogType> pair : BioConverterNodeModel
						.possibleKeys()) {
					setPattern(profile.getName(), pair.getLeft().getLeft(),
							pair.getLeft().getRight().booleanValue(), pair
									.getRight(), ancestor.get(
									pair.getLeft().getLeft()).get(
									pair.getLeft().getRight()).get(
									pair.getRight()));
				}
			}
			for (final Value value : profile.getValue()) {
				setPattern(profile.getName(), value.getColumn(), profile
						.isInput(), value.getDialog(), value.getValue());
			}
		}
	}

	private static Map<ColumnType, Map<Boolean, Map<DialogType, String>>> createOuterEmptyMap() {
		final Map<ColumnType, Map<Boolean, Map<DialogType, String>>> ret = new EnumMap<ColumnType, Map<Boolean, Map<DialogType, String>>>(
				ColumnType.class);
		for (final ColumnType c : ColumnType.values()) {
			ret.put(c, createEmptyMap());
		}
		return ret;
	}

	private static void setPattern(final String profileName,
			final ColumnType col, final boolean input, final DialogType type,
			final String value) {
		patterns.get(profileName).get(col).get(Boolean.valueOf(input)).put(
				type, value);
	}

	private static Map<Boolean, Map<DialogType, String>> createEmptyMap() {
		final Map<Boolean, Map<DialogType, String>> ret = new HashMap<Boolean, Map<DialogType, String>>();
		final Map<DialogType, String> left = new EnumMap<DialogType, String>(
				DialogType.class);
		final Map<DialogType, String> right = new EnumMap<DialogType, String>(
				DialogType.class);
		ret.put(Boolean.TRUE, left);
		ret.put(Boolean.FALSE, right);
		return ret;
	}

	/**
	 * New pane for configuring the BioConverter node.
	 */
	protected BioConverterNodeDialog() {
		super();
		setDefaultTabTitle("General");
		final Map<String, Boolean[]> genInEnablementMap = new LinkedHashMap<String, Boolean[]>();
		// key: name of concept
		final Map<ColumnType, Pair<Map<DialogType, DialogComponent>, Map<DialogType, DialogComponent>>> components = new LinkedHashMap<ColumnType, Pair<Map<DialogType, DialogComponent>, Map<DialogType, DialogComponent>>>();
		addSimpleComponents(components);
		addPatterns(components);
		for (final Profile p : BioConverterNodeModel.root.getProfiles()
				.getProfile()) {
			genInEnablementMap.put(p.getName(), fill(Boolean.FALSE, components
					.size(), Boolean.class));
		}
		genInEnablementMap.put(ConversionDefault.custom.getDisplayText(), fill(
				Boolean.TRUE, components.size(), Boolean.class));
		final Map<String, Object[]> genInDefaultsMap = new LinkedHashMap<String, Object[]>();
		for (final Profile p : BioConverterNodeModel.root.getProfiles()
				.getProfile()) {
			genInDefaultsMap.put(p.getName(), fill(p.getName(), components
					.size(), String.class));
		}
		final DialogComponentWithDefaults dialogGeneralIn = new DialogComponentWithDefaults(
				new SettingsModelString(
						BioConverterNodeModel.CFGKEY_GENERAL_IN_GROUP,
						BioConverterNodeModel.DEFAULT_GENERAL_IN_GROUP),
				"Input", genInEnablementMap, genInDefaultsMap, select(
						components, DialogType.group, true));
		final DialogComponentWithDefaults dialogGeneralOut = new DialogComponentWithDefaults(
				new SettingsModelString(
						BioConverterNodeModel.CFGKEY_GENERAL_OUT_GROUP,
						BioConverterNodeModel.DEFAULT_GENERAL_OUT_GROUP),
				"Output", genInEnablementMap, genInDefaultsMap, select(
						components, DialogType.group, false));
		createNewGroup("Conversion");
		addDialogComponent(dialogGeneralIn);
		addDialogComponent(dialogGeneralOut);
		createNewGroup("Other options");
		addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(
				BioConverterNodeModel.CFGKEY_ADD_UNMATCHED,
				BioConverterNodeModel.DEFAULT_ADD_UNMATCHED),
				"Add unmatched columns?"));
		addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(
				BioConverterNodeModel.CFGKEY_KEEP_ORIGINAL,
				BioConverterNodeModel.DEFAULT_KEEP_ORIGINAL),
				"Keep original columns?"));
		addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(
				BioConverterNodeModel.CFGKEY_GENERATE_MISSING,
				BioConverterNodeModel.DEFAULT_GENERATE_MISSING),
				"Try generate missing column values?"));
		createTab(components, ColumnType.Plate);
		createTab(components, ColumnType.Replicate);
	}

	/**
	 * Creates a {@link Map} from the profile names to the enablements.
	 * 
	 * @param nonGroups
	 *            The non-{@link DialogType#group}s.
	 * @return The map.
	 */
	private Map<String, Boolean[]> createEnablementMap(
			final DialogType[] nonGroups) {
		final Map<String, Boolean[]> enablementMap = new HashMap<String, Boolean[]>();
		ConversionDefault.values();
		for (final Profile p : BioConverterNodeModel.root.getProfiles()
				.getProfile()) {
			enablementMap.put(p.getName(), fill(Boolean.FALSE,
					nonGroups.length, Boolean.class));
		}
		enablementMap.put(ConversionDefault.custom.getDisplayText(), fill(
				Boolean.TRUE, nonGroups.length, Boolean.class));
		return enablementMap;
	}

	/**
	 * Adds the {@link DialogType#group} components to {@code components}.
	 * Assumes that the non-{@link DialogType#group} components are already
	 * added.
	 * 
	 * @param components
	 *            The {@link DialogComponent} group. (in-out)
	 */
	private void addPatterns(
			final Map<ColumnType, Pair<Map<DialogType, DialogComponent>, Map<DialogType, DialogComponent>>> components) {
		for (final Pair<Pair<ColumnType, Boolean>, DialogType> pair : BioConverterNodeModel
				.possibleKeys()) {
			if (pair.getRight() == DialogType.group) {
				final ColumnType columnType = pair.getLeft().getLeft();
				final boolean input = pair.getLeft().getRight().booleanValue();
				final DialogType[] nonGroups = DialogType.nonGroups(input);
				final DialogComponentWithDefaults groupComponent = new DialogComponentWithDefaults(
						new SettingsModelString(
								BioConverterNodeModel.generateKey(columnType,
										input, pair.getRight()),
								BioConverterNodeModel.findDefault(columnType,
										input, pair.getRight())),
						DialogType.group.name(),
						createEnablementMap(nonGroups), collectPatterns(
								columnType, pair.getLeft().getRight()
										.booleanValue()), select(components,
								columnType, Arrays.asList(nonGroups), input));
				final Pair<Map<DialogType, DialogComponent>, Map<DialogType, DialogComponent>> p = components
						.get(columnType);
				(input ? p.getLeft() : p.getRight()).put(DialogType.group,
						groupComponent);
			}
		}
	}

	/**
	 * Collects the patterns for the {@code columnType, input} parameters from
	 * {@link #patterns}.
	 * 
	 * @param columnType
	 *            The {@link ColumnType}.
	 * @param input
	 *            Input ({@code true}) or output ({@code false}) kind.
	 * @return The collected patterns in order of
	 *         {@link DialogType#nonGroups(boolean)}.
	 * 
	 * @see DialogType#nonGroups(boolean)
	 */
	private Map<String, Object[]> collectPatterns(final ColumnType columnType,
			final boolean input) {
		final DialogType[] nonGroups = DialogType.nonGroups(input);
		final Map<String, Object[]> ret = new LinkedHashMap<String, Object[]>();
		for (final Entry</* ConversionDefault */String, Map<ColumnType, Map<Boolean, Map<DialogType, String>>>> entry : patterns
				.entrySet()) {
			final String displayText = entry.getKey()/* .getDisplayText() */;
			final String[] value = new String[nonGroups.length];
			for (int i = nonGroups.length; i-- > 0;) {
				value[i] = entry.getValue().get(columnType).get(
						Boolean.valueOf(input)).get(nonGroups[i]);
			}
			ret.put(displayText, value);
		}
		return ret;
	}

	/**
	 * Adds the simple components (non-{@link DialogType#group}) to {@code
	 * components}.
	 * 
	 * @param components
	 *            The {@link Map} to add the new {@link DialogComponent}s.
	 */
	private void addSimpleComponents(
			final Map<ColumnType, Pair<Map<DialogType, DialogComponent>, Map<DialogType, DialogComponent>>> components) {
		for (final ColumnType colType : ColumnType.values()) {
			components
					.put(
							colType,
							new Pair<Map<DialogType, DialogComponent>, Map<DialogType, DialogComponent>>(
									new EnumMap<DialogType, DialogComponent>(
											DialogType.class),
									new EnumMap<DialogType, DialogComponent>(
											DialogType.class)));
			for (final boolean left : new boolean[] { true, false }) {
				for (final DialogType dialogType : DialogType.nonGroups(left)) {
					final Pair<Map<DialogType, DialogComponent>, Map<DialogType, DialogComponent>> pair = components
							.get(colType);
					final String configName = BioConverterNodeModel
							.generateKey(colType, left, dialogType);
					final String defaultValue = BioConverterNodeModel
							.findDefault(colType, left, dialogType);
					final boolean colSelection = dialogType == DialogType.name
							&& left;
					final SettingsModelString stringModel = colSelection ? new SettingsModelColumnName(
							configName, defaultValue)
							: new SettingsModelString(configName, defaultValue);
					final String label = (left ? "input " : "output ")
							+ colType.getDisplayText() + " "
							+ dialogType.name() + ": ";
					DialogComponent dialog;
					switch (dialogType) {
					case name:
						dialog = colSelection ? createColumnNameSelector(
								stringModel, label)
								: new DialogComponentString(stringModel, label,
										false, 15);
						break;
					case format:
						dialog = new DialogComponentString(stringModel, label,
								false, 30);
						break;
					case type:
						dialog = new DialogComponentStringSelection(
								stringModel, label, "Integer", "Real", "String");
						break;
					case group:
						throw new IllegalStateException(
								"group is not allowed here");
					default:
						throw new UnsupportedOperationException(
								"Not supported type: " + dialogType.getClass());
					}
					(left ? pair.getLeft() : pair.getRight()).put(dialogType,
							dialog);
				}
			}
		}
	}

	/**
	 * Creates a new {@link DialogComponentColumnNameSelection} from the first
	 * inport.
	 * 
	 * @param stringModel
	 *            The associated {@link SettingsModelString}.
	 * @param label
	 *            The label to show.
	 * @return A new {@link DialogComponentColumnNameSelection}.
	 */
	@SuppressWarnings("unchecked")
	private DialogComponentColumnNameSelection createColumnNameSelector(
			final SettingsModelString stringModel, final String label) {
		return new DialogComponentColumnNameSelection(stringModel, label, 0,
				IntValue.class, StringValue.class);
	}

	/**
	 * Creates a tab with the proper {@link DialogComponent}s.
	 * <p>
	 * Use only in the constructor.
	 * 
	 * @param components
	 *            Group of {@link DialogComponent}s.
	 * @param columnType
	 *            The selected {@link ColumnType}.
	 */
	private void createTab(
			final Map<ColumnType, Pair<Map<DialogType, DialogComponent>, Map<DialogType, DialogComponent>>> components,
			final ColumnType columnType) {
		createNewTab(columnType.getDisplayText());
		createNewGroup("Input");
		setHorizontalPlacement(false);
		addDialogComponent(select(columnType, components, DialogType.group,
				true));
		setHorizontalPlacement(true);
		addDialogComponent(select(columnType, components, DialogType.name, true));
		addDialogComponent(select(columnType, components, DialogType.format,
				true));
		createNewGroup("Output");
		setHorizontalPlacement(false);
		addDialogComponent(select(columnType, components, DialogType.group,
				false));
		setHorizontalPlacement(true);
		addDialogComponent(select(columnType, components, DialogType.name,
				false));
		addDialogComponent(select(columnType, components, DialogType.format,
				false));
		addDialogComponent(select(columnType, components, DialogType.type,
				false));
	}

	/**
	 * Selects the proper {@link DialogComponent} from the {@code components}.
	 * 
	 * @param colType
	 *            The {@link ColumnType} of the query.
	 * @param components
	 *            The group of {@link DialogComponent}s.
	 * @param type
	 *            The {@link DialogType} to find.
	 * @param input
	 *            Input ({@code true}) or output ({@code false}) kind.
	 * @return The found {@link DialogComponent}.
	 */
	private DialogComponent select(
			final ColumnType colType,
			final Map<ColumnType, Pair<Map<DialogType, DialogComponent>, Map<DialogType, DialogComponent>>> components,
			final DialogType type, final boolean input) {
		final Pair<Map<DialogType, DialogComponent>, Map<DialogType, DialogComponent>> pair = components
				.get(colType);
		final DialogComponent ret = (input ? pair.getLeft() : pair.getRight())
				.get(type);
		if (ret == null) {
			throw new NullPointerException();
		}
		return ret;
	}

	/**
	 * Selects the {@link DialogComponent}s with the query type ({@code type})
	 * and input kind from {@code components}.
	 * 
	 * @param components
	 *            The group of {@link DialogComponent}s.
	 * @param type
	 *            The preferred {@link DialogType}.
	 * @param input
	 *            Input ({@code true}) or output ({@code false}) kind.
	 * @return The {@link DialogComponent}s belonging to the specified
	 *         parameters.
	 */
	private static DialogComponent[] select(
			final Map<ColumnType, Pair<Map<DialogType, DialogComponent>, Map<DialogType, DialogComponent>>> components,
			final DialogType type, final boolean input) {
		final DialogComponent[] ret = new DialogComponent[components.size()];
		int i = 0;
		for (final Entry<ColumnType, Pair<Map<DialogType, DialogComponent>, Map<DialogType, DialogComponent>>> entry : components
				.entrySet()) {
			final Pair<Map<DialogType, DialogComponent>, Map<DialogType, DialogComponent>> value = entry
					.getValue();
			final Map<DialogType, DialogComponent> map = input ? value
					.getLeft() : value.getRight();
			ret[i++] = map.get(type);
		}
		return ret;
	}

	/**
	 * Selects the proper {@link DialogComponent}s. These dialogComponents are
	 * in order of {@code types}.
	 * 
	 * @param components
	 *            The mapping containing all existing {@link DialogComponent}s.
	 * @param columnType
	 *            The {@link ColumnType} we are looking for.
	 * @param types
	 *            The {@link DialogType}s to select.
	 * @param input
	 *            We are looking for input-like {@link DialogComponent}s (
	 *            {@code true}), or output-like ones ({@code false}).
	 * @return The {@link DialogComponent}s belonging to the selection
	 *         parameters.
	 */
	private static DialogComponent[] select(
			final Map<ColumnType, Pair<Map<DialogType, DialogComponent>, Map<DialogType, DialogComponent>>> components,
			final ColumnType columnType, final List<DialogType> types,
			final boolean input) {
		final DialogComponent[] ret = new DialogComponent[types.size()];
		int i = 0;
		final Pair<Map<DialogType, DialogComponent>, Map<DialogType, DialogComponent>> value = components
				.get(columnType);
		final Map<DialogType, DialogComponent> map = input ? value.getLeft()
				: value.getRight();
		for (final DialogType type : types) {
			ret[i++] = map.get(type);
		}
		return ret;
	}

	private static <T> T[] fill(final T init, final int length,
			final Class<T> cls) {
		@SuppressWarnings("unchecked")
		final T[] ret = (T[]) Array.newInstance(cls, length);
		Arrays.fill(ret, init);
		return ret;
	}
}
