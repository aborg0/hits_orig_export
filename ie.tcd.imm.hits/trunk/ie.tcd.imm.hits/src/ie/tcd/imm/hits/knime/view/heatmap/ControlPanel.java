/**
 * 
 */
package ie.tcd.imm.hits.knime.view.heatmap;

import ie.tcd.imm.hits.knime.view.heatmap.ControlPanel.Slider.SliderFactory;
import ie.tcd.imm.hits.knime.view.heatmap.ControlPanel.Slider.Type;
import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeModel.StatTypes;
import ie.tcd.imm.hits.knime.view.heatmap.HeatmapNodeView.VolatileModel;
import ie.tcd.imm.hits.knime.view.heatmap.ViewModel.Format;
import ie.tcd.imm.hits.knime.view.heatmap.ViewModel.ParameterModel;
import ie.tcd.imm.hits.knime.view.heatmap.ViewModel.Shape;
import ie.tcd.imm.hits.knime.view.heatmap.ViewModel.ShapeModel;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicBorders;

/**
 * With this panel you can control the appearance of the heatmap's circles
 * views.
 * 
 * @author <a href="mailto:bakosg@tcd.ie">Gabor Bakos</a>
 */
public class ControlPanel extends JPanel {
	private static final long serialVersionUID = -96828595837428105L;

	private final ButtonGroup heatMapFormatGroup = new ButtonGroup();
	private final ButtonGroup shapeGroup = new ButtonGroup();

	private final JCheckBox showBorderButton = new JCheckBox("Show border");
	private final JCheckBox showPrimarySeparatorButton = new JCheckBox(
			"Show slice separators");
	private final JCheckBox showSecundarySeparatorButton = new JCheckBox(
			"Show circle separators");
	private final JCheckBox showAdditionalSeparatorButton = new JCheckBox(
			"Show rectangle");

	private final JRadioButton _96;

	private final JRadioButton _384;

	private final JRadioButton circle;

	private final JRadioButton rectangle;

	private final LegendPanel legendPanel;

	/**
	 * This is something that represents a {@link ParameterModel} list and
	 * values.
	 */
	static class Slider implements Serializable {
		private static final long serialVersionUID = 8868671426882187720L;

		/**
		 * The position of the {@link Slider} in the window.
		 */
		static enum Type {
			/**
			 * The {@link Slider} is not visible, only settable from the control
			 * screen
			 */
			Hidden,
			/** The {@link Slider} splits the wells */
			Splitter,
			/** The {@link Slider} is distributed across the vertical scrollbar */
			ScrollVertical,
			/** The {@link Slider} is distributed across the horizontal scrollbar */
			ScrollHorisontal,
			/** The {@link Slider} values are on the selector panel. */
			Selector;
		}

		public static final int MAX_INDEPENDENT_FACTORS = 3;

		private final int subId;

		private final Type type;

		private final List<ParameterModel> parameters = new ArrayList<ParameterModel>();
		private final Map<Integer, Map<ParameterModel, Object>> valueMapping = new HashMap<Integer, Map<ParameterModel, Object>>();

		public static class SliderFactory {
			private final Set<Slider> sliders = new HashSet<Slider>();

			public Set<Slider> get(final Type type,
					final List<ParameterModel> parameters,
					final Map<Integer, Map<ParameterModel, Object>> valueMapping) {
				final Set<Slider> ret = new HashSet<Slider>();
				for (final Slider slider : sliders) {
					if (slider.type == type
							&& parameters.equals(slider.parameters)
							&& valueMapping.equals(slider.valueMapping)) {
						ret.add(slider);
					}
				}
				if (ret.isEmpty()) {
					final Slider slider = new Slider(type, 0, parameters,
							valueMapping);
					sliders.add(slider);
					ret.add(slider);
				}
				return ret;
			}
		}

		private Slider(final Type type, final int subId,
				final List<ParameterModel> parameters,
				final Map<Integer, Map<ParameterModel, Object>> valueMapping) {
			super();
			this.type = type;
			this.subId = subId;
			this.parameters.addAll(parameters);
			this.valueMapping.putAll(valueMapping);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((parameters == null) ? 0 : parameters.hashCode());
			result = prime * result + subId;
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			result = prime * result
					+ ((valueMapping == null) ? 0 : valueMapping.hashCode());
			return result;
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final Slider other = (Slider) obj;
			if (parameters == null) {
				if (other.parameters != null) {
					return false;
				}
			} else if (!parameters.equals(other.parameters)) {
				return false;
			}
			if (subId != other.subId) {
				return false;
			}
			if (type == null) {
				if (other.type != null) {
					return false;
				}
			} else if (type != other.type) {
				return false;
			}
			if (valueMapping == null) {
				if (other.valueMapping != null) {
					return false;
				}
			} else if (!valueMapping.equals(other.valueMapping)) {
				return false;
			}
			return true;
		}

		public int getSubId() {
			return subId;
		}

		public Type getType() {
			return type;
		}

		public List<ParameterModel> getParameters() {
			return Collections.unmodifiableList(parameters);
		}

		public Map<Integer, Map<ParameterModel, Object>> getValueMapping() {
			return valueMapping;
		}

		@Override
		public String toString() {
			return type + "_" + subId + " " + parameters;
		}
	}

	static class ArrangementModel implements Serializable, ActionListener {
		private static final long serialVersionUID = -3108970660588264496L;

		private final SliderFactory factory = new SliderFactory();

		private final EnumMap<Type, Collection<Slider>> sliders = new EnumMap<Type, Collection<Slider>>(
				Type.class);

		private final Map<StatTypes, Collection<String>> typeValues = new EnumMap<StatTypes, Collection<String>>(
				StatTypes.class);

		private final LinkedHashMap<ParameterModel, Collection<Slider>> mainArrangement = new LinkedHashMap<ParameterModel, Collection<Slider>>();

		private final List<ActionListener> listeners = new ArrayList<ActionListener>();

		public ArrangementModel() {
			super();
		}

		public Map<Type, Collection<Slider>> getSliders() {
			final EnumMap<Type, Collection<Slider>> ret = new EnumMap<Type, Collection<Slider>>(
					Type.class);
			for (final Map.Entry<Type, Collection<Slider>> entry : sliders
					.entrySet()) {
				ret.put(entry.getKey(), Collections
						.unmodifiableCollection(entry.getValue()));
			}
			return Collections.unmodifiableMap(ret);
		}

		public void mutate(final Collection<ParameterModel> possibleParameters) {
			// TODO mutate the current arrangement, instead of creating new
			// one...
			typeValues.clear();
			for (final StatTypes type : StatTypes.values()) {
				typeValues.put(type, new TreeSet<String>());
			}
			for (final Type type : Type.values()) {
				sliders.put(type, new ArrayList<Slider>());
			}
			ParameterModel parameters = null;
			ParameterModel plateModel = null;
			ParameterModel replicateModel = null;
			ParameterModel stats = null;
			final Set<ParameterModel> knownStats = new HashSet<ParameterModel>();
			for (final ParameterModel parameterModel : possibleParameters) {
				typeValues.get(parameterModel.getType()).add(
						parameterModel.getShortName());
				switch (parameterModel.getType()) {
				case parameter:
					parameters = parameterModel;
					break;
				case plate:
					plateModel = parameterModel;
					break;
				case replicate:
					replicateModel = parameterModel;
					break;
				case meanOrDiff:
				case median:
				case normalized:
				case raw:
				case rawPerMedian:
				case score:
					knownStats.add(parameterModel);
					break;
				case metaStatType:
					stats = parameterModel;
					break;
				default:
					break;
				}
			}
			if (replicateModel == null) {
				replicateModel = new ParameterModel("replicate",
						StatTypes.replicate, null, Collections
								.<String> emptyList(), Collections
								.singletonList(Integer.toString(1)));
				replicateModel.getColorLegend().put(Integer.valueOf(1),
						Color.BLACK);
			}
			for (final StatTypes type : StatTypes.values()) {
				if (typeValues.get(type).isEmpty()) {
					typeValues.remove(type);
				}
			}
			final Slider paramsSlider;
			{
				final Map<Integer, Map<ParameterModel, Object>> parametersMapping = new TreeMap<Integer, Map<ParameterModel, Object>>();
				int i = 1;
				for (final Object o : parameters.getColorLegend().keySet()) {
					if (o instanceof String) {
						final String name = (String) o;
						parametersMapping.put(Integer.valueOf(i++), Collections
								.singletonMap(parameters, (Object) name));
					}
				}
				final Set<Slider> set = factory.get(Type.Splitter, Collections
						.singletonList(parameters), parametersMapping);
				assert !set.isEmpty();
				sliders.get(Type.Splitter).add(
						paramsSlider = set.iterator().next());
			}
			final Slider plateSlider;
			{
				final Map<Integer, Map<ParameterModel, Object>> plateMapping = new TreeMap<Integer, Map<ParameterModel, Object>>();
				for (final Object o : plateModel.getColorLegend().keySet()) {
					if (o instanceof Integer) {
						final Integer i = (Integer) o;
						plateMapping.put(i, Collections.singletonMap(
								plateModel, (Object) i));
					}
				}
				final Set<Slider> plateSet = factory.get(Type.Selector,
						Collections.singletonList(plateModel), plateMapping);
				assert !plateSet.isEmpty();
				sliders.get(Type.Selector).add(
						plateSlider = plateSet.iterator().next());
			}
			final Slider replicateSlider;
			{
				final Map<Integer, Map<ParameterModel, Object>> replicateMapping = new TreeMap<Integer, Map<ParameterModel, Object>>();
				for (final Object o : replicateModel.getColorLegend().keySet()) {
					if (o instanceof Integer) {
						final Integer i = (Integer) o;
						replicateMapping.put(i, Collections.singletonMap(
								replicateModel, (Object) i));
					}
				}
				if (replicateMapping.isEmpty()) {
				}
				final Set<Slider> replicateSet = factory.get(Type.Splitter,
						Collections.singletonList(replicateModel),
						replicateMapping);
				assert !replicateSet.isEmpty();
				sliders.get(Type.Splitter).add(
						replicateSlider = replicateSet.iterator().next());
			}
			Slider statSlider;
			{
				int i = 1;
				final Map<Integer, Map<ParameterModel, Object>> statMapping = new TreeMap<Integer, Map<ParameterModel, Object>>();
				for (final String statName : stats.getColumnValues()) {
					statMapping.put(i++, Collections.singletonMap(stats,
							(Object) StatTypes.valueOf(statName)));
				}
				final Set<Slider> statSet = factory.get(Type.Hidden,
						Collections.singletonList(stats), statMapping);
				assert !statSet.isEmpty();
				sliders.get(Type.Hidden).add(
						statSlider = statSet.iterator().next());
			}
			final ArrayList<Slider> mainSliders = new ArrayList<Slider>();
			mainSliders.add(statSlider);
			mainSliders.add(plateSlider);
			mainSliders.add(replicateSlider);
			mainSliders.add(paramsSlider);
			mainArrangement.put(parameters, mainSliders);
			// sliders.get(Type.Hidden).add(statSlider);
			// sliders.get(Type.Selector).add(plateSlider);
			// sliders.get(Type.Splitter).add(replicateSlider);
			// sliders.get(Type.Splitter).add(paramsSlider);
			// for (final Collection<Slider> sliderColl : sliders.values()) {
			// for (final Slider slider : sliderColl) {
			// view.getVolatileModel().setSliderPosition(slider,
			// Integer.valueOf(1));
			// }
			// }
		}

		public Map<StatTypes, Collection<String>> getTypeValuesMap() {
			return typeValues;
		}

		public LinkedHashMap<ParameterModel, Collection<Slider>> getMainArrangement() {
			return mainArrangement;
		}

		public void addListener(final ActionListener listener) {
			listeners.add(listener);
		}

		public void removeListener(final ActionListener listener) {
			listeners.remove(listener);
		}

		static Slider selectNth(
				final LinkedHashMap<ParameterModel, Collection<Slider>> mainArrangement,
				final int i, final StatTypes plate) {
			int u = 0;
			for (final Map.Entry<ParameterModel, Collection<Slider>> entry : mainArrangement
					.entrySet()) {
				if (u++ == i) {
					final Collection<Slider> sliders = entry.getValue();
					for (final Slider slider : sliders) {
						for (final ParameterModel param : slider
								.getParameters()) {
							if (param.getType() == plate) {
								// Not sure whether this is necessary.
								assert slider.getParameters().size() == 1;
								return slider;
							}
						}
					}
				}
			}
			return null;
		}

		@Override
		public String toString() {
			return mainArrangement.toString();
		}

		public void addValue(final Slider slider, final Integer origKey,
				final Map<ParameterModel, Object> map) {
			// TODO Auto-generated method stub

		}

		public void removeValue(final Slider slider, final Integer origKey) {
			// TODO Auto-generated method stub

		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			for (final ActionListener listener : listeners) {
				listener.actionPerformed(e);
			}
		}
	}

	private static class ParamaterSelection extends JPanel {
		private static final long serialVersionUID = 5247512869526999773L;

		private final JComboBox typeCombobox = new JComboBox(StatTypes.values());

		private final JComboBox valueCombobox = new JComboBox();

		private final EnumMap<StatTypes, Collection<String>> possibleValues = new EnumMap<StatTypes, Collection<String>>(
				StatTypes.class);
		{
			typeCombobox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					final StatTypes selectedType = (StatTypes) typeCombobox
							.getSelectedItem();
					valueCombobox.removeAllItems();
					for (final String value : possibleValues.get(selectedType)) {
						valueCombobox.addItem(value);
					}
				}
			});
		}

		public ParamaterSelection() {
			super();
			add(typeCombobox);
			add(valueCombobox);
			setBorder(new BasicBorders.MarginBorder());
		}

		public void setPossibleValues(
				final Map<StatTypes, Collection<String>> possValues) {
			possibleValues.clear();
			for (final Map.Entry<StatTypes, Collection<String>> entry : possValues
					.entrySet()) {
				possibleValues.put(entry.getKey(), new ArrayList<String>(entry
						.getValue()));
			}
		}
	}

	private final ParamaterSelection paramaterSelection;

	private final HeatmapNodeView view;

	private final JPanel hiddenSliders = new JPanel();

	private final JPanel primarySliders = new JPanel();
	private final JPanel secundarySliders = new JPanel();
	private final JPanel additionalSliders = new JPanel();

	public ControlPanel(final HeatmapNodeView origView) {
		super();
		this.view = origView;
		final GridBagLayout gbLayout = new GridBagLayout();
		setLayout(gbLayout);
		final GridBagConstraints formatConstraints = new GridBagConstraints();
		final GridBagConstraints paramSelectConstraints = formatConstraints;
		paramaterSelection = new ParamaterSelection();
		gbLayout.addLayoutComponent(paramaterSelection, paramSelectConstraints);
		add(paramaterSelection, paramSelectConstraints);
		final JPanel heatmapFormatPanel = new JPanel();
		final BasicBorders.RadioButtonBorder hmBorder = new BasicBorders.RadioButtonBorder(
				Color.LIGHT_GRAY, Color.DARK_GRAY, Color.YELLOW, Color.ORANGE);
		heatmapFormatPanel.setBorder(new TitledBorder(hmBorder, "plate type"));
		_96 = new JRadioButton("96", true);
		heatmapFormatPanel.add(_96);
		_384 = new JRadioButton("384", false);
		heatmapFormatPanel.add(_384);
		heatMapFormatGroup.add(_96);
		heatMapFormatGroup.add(_384);
		final JPanel shapePanel = new JPanel();
		circle = new JRadioButton("circle", true);
		rectangle = new JRadioButton("rectangle", true);
		shapeGroup.add(circle);
		shapeGroup.add(rectangle);
		shapePanel.add(circle);
		shapePanel.add(rectangle);
		shapePanel.setBorder(new TitledBorder(
				new BasicBorders.RadioButtonBorder(Color.LIGHT_GRAY,
						Color.DARK_GRAY, Color.YELLOW, Color.ORANGE), "shape"));
		final ActionListener actionListener = new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				origView.changeView(
						_96.isSelected() ? Format._96 : Format._384, circle
								.isSelected() ? Shape.Circle : Shape.Rectangle);
				updateControl(origView.getCurrentViewModel());
			}
		};
		_96.addActionListener(actionListener);
		_384.addActionListener(actionListener);
		circle.addActionListener(actionListener);
		rectangle.addActionListener(actionListener);

		formatConstraints.gridx = 1;
		gbLayout.addLayoutComponent(heatmapFormatPanel, formatConstraints);
		add(heatmapFormatPanel, formatConstraints);
		final GridBagConstraints shapeConstraints = new GridBagConstraints();
		shapeConstraints.gridx = 1;
		shapeConstraints.gridy = 1;
		gbLayout.addLayoutComponent(shapePanel, shapeConstraints);
		add(shapePanel, shapeConstraints);
		addBorderButton(origView, gbLayout, showBorderButton, 1, 2, 0);
		addBorderButton(origView, gbLayout, showPrimarySeparatorButton, 2, 2, 1);
		addBorderButton(origView, gbLayout, showSecundarySeparatorButton, 3, 2,
				2);
		addBorderButton(origView, gbLayout, showAdditionalSeparatorButton, 4,
				2, 3);
		legendPanel = new LegendPanel(true, origView.getCurrentViewModel());
		final GridBagConstraints legendConstraints = new GridBagConstraints();
		legendConstraints.gridx = 4;
		legendConstraints.gridheight = 7;
		gbLayout.addLayoutComponent(legendPanel, legendConstraints);
		add(legendPanel, legendConstraints);
		final GridBagConstraints hiddenSlidersConstraints = new GridBagConstraints();
		hiddenSlidersConstraints.gridx = 1;
		hiddenSlidersConstraints.gridy = 5;
		gbLayout.addLayoutComponent(hiddenSliders, hiddenSlidersConstraints);
		add(hiddenSliders, hiddenSlidersConstraints);
		final GridBagConstraints primaryConstraints = new GridBagConstraints();
		primaryConstraints.gridx = 1;
		primaryConstraints.gridy = 2;
		gbLayout.addLayoutComponent(primarySliders, primaryConstraints);
		add(primarySliders, primaryConstraints);
		final GridBagConstraints secundaryConstraints = new GridBagConstraints();
		secundaryConstraints.gridx = 1;
		secundaryConstraints.gridy = 3;
		gbLayout.addLayoutComponent(secundarySliders, secundaryConstraints);
		add(secundarySliders, secundaryConstraints);
		final GridBagConstraints additionalConstraints = new GridBagConstraints();
		additionalConstraints.gridx = 1;
		additionalConstraints.gridy = 4;
		gbLayout.addLayoutComponent(additionalSliders, additionalConstraints);
		add(additionalSliders, additionalConstraints);
	}

	protected void updateControl(final ViewModel currentViewModel) {
		switch (currentViewModel.getShape()) {
		case Circle:
			shapeGroup.setSelected(circle.getModel(), true);
			shapeGroup.setSelected(rectangle.getModel(), false);
			showPrimarySeparatorButton.setText("Show slice separators");
			showSecundarySeparatorButton.setText("Show circle separators");
			showAdditionalSeparatorButton.setVisible(true);
			showAdditionalSeparatorButton.setText("Show rectangle");
			break;
		case Rectangle:
			shapeGroup.setSelected(rectangle.getModel(), true);
			shapeGroup.setSelected(circle.getModel(), false);
			showPrimarySeparatorButton.setText("Show vertical lines");
			showSecundarySeparatorButton.setText("Show horizontal lines");
			showAdditionalSeparatorButton.setVisible(false);
			break;
		default:
			throw new UnsupportedOperationException("Not supported: "
					+ currentViewModel.getShape());
		}
		switch (currentViewModel.getFormat()) {
		case _96:
			heatMapFormatGroup.setSelected(_96.getModel(), true);
			heatMapFormatGroup.setSelected(_384.getModel(), false);
			break;
		case _384:
			heatMapFormatGroup.setSelected(_384.getModel(), true);
			heatMapFormatGroup.setSelected(_96.getModel(), false);
			break;
		default:
			throw new UnsupportedOperationException("Not supported: "
					+ currentViewModel.getFormat());
		}
		setSelection(currentViewModel.getMain(), showBorderButton, 0);
		setSelection(currentViewModel.getMain(), showPrimarySeparatorButton, 1);
		setSelection(currentViewModel.getMain(), showSecundarySeparatorButton,
				2);
		setSelection(currentViewModel.getMain(), showAdditionalSeparatorButton,
				3);
		legendPanel.setViewModel(currentViewModel);
		final ArrangementModel arrangementModel = currentViewModel.getMain()
				.getArrangementModel();
		// Update hidden sliders
		{
			final Collection<Slider> sliders = arrangementModel.getSliders()
					.get(Type.Hidden);
			hiddenSliders.removeAll();
			final GridBagLayout gridBagLayout = new GridBagLayout();
			hiddenSliders.setLayout(gridBagLayout);
			final int[] counts = new int[Slider.MAX_INDEPENDENT_FACTORS];
			for (final Slider slider : sliders) {
				final int pos = ++counts[slider.getSubId()];
				final GridBagConstraints constraint = new GridBagConstraints();
				constraint.gridx = slider.getSubId();
				constraint.gridy = pos;
				hiddenSliders.add(createSliderComboBox(slider), constraint);
			}
		}
		{
			final Collection<Slider> sliders = arrangementModel.getSliders()
					.get(Type.Splitter);
			final List<ParameterModel> primerParameters = currentViewModel
					.getMain().getPrimerParameters();
			primarySliders.removeAll();
			final GridLayout gridBagLayout = new GridLayout(1, primerParameters
					.size());
			primarySliders.setLayout(gridBagLayout);
			for (final ParameterModel parameterModel : primerParameters) {
				for (final Slider possSlider : sliders) {
					final List<ParameterModel> parameters = possSlider
							.getParameters();
					if (parameters.size() == 1) {
						final ParameterModel possParameter = parameters
								.iterator().next();
						if (possParameter.getType() == parameterModel.getType()) {
							primarySliders.add(createSliderModifier(
									currentViewModel, possSlider, view
											.getVolatileModel()));
						}
					} else {
						// TODO ??
					}
				}
			}
		}
		{
			final Collection<Slider> sliders = arrangementModel.getSliders()
					.get(Type.Splitter);
			final List<ParameterModel> secundaryParameters = currentViewModel
					.getMain().getSecunderParameters();
			secundarySliders.removeAll();
			final GridLayout gridBagLayout = new GridLayout(1,
					secundaryParameters.size());
			secundarySliders.setLayout(gridBagLayout);
			for (final ParameterModel parameterModel : secundaryParameters) {
				for (final Slider possSlider : sliders) {
					final List<ParameterModel> parameters = possSlider
							.getParameters();
					if (parameters.size() == 1) {
						final ParameterModel possParameter = parameters
								.iterator().next();
						if (possParameter.getType() == parameterModel.getType()) {
							secundarySliders.add(createSliderModifier(
									currentViewModel, possSlider, view
											.getVolatileModel()));
						}
					} else {
						// TODO ??
					}
				}
			}
		}
		// currentViewModel.getMain().getArrangementModel().addListener(
		// legendPanel);
	}

	private Component createSliderModifier(final ViewModel viewModel,
			final Slider slider, final VolatileModel volatileModel) {
		final ArrangementModel arrangementModel = viewModel.getMain()
				.getArrangementModel();
		final List<ParameterModel> parameters = slider.getParameters();
		final JPanel ret = new JPanel();
		if (parameters.size() == 1) {
			final ParameterModel model = parameters.iterator().next();
			ret.setBorder(new TitledBorder(model.getShortName()));
			for (final Entry<Integer, Map<ParameterModel, Object>> entry : slider
					.getValueMapping().entrySet()) {
				final String val = entry.getValue().get(model).toString();
				final JToggleButton button = new JToggleButton(val);
				button.setSelected(true);
				final Integer origKey = entry.getKey();
				final LinkedHashMap<Integer, Map<ParameterModel, Object>> originalMap = new LinkedHashMap<Integer, Map<ParameterModel, Object>>(
						slider.getValueMapping());
				button.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(final ActionEvent e) {
						if (button.isSelected()) {
							for (final Entry<ParameterModel, Collection<Slider>> entry : arrangementModel
									.getMainArrangement().entrySet()) {
								for (final Slider otherSlider : entry
										.getValue()) {
									if (otherSlider.equals(slider)) {
										otherSlider.getValueMapping().put(
												origKey,
												originalMap.get(origKey));
									}
								}
							}
							slider.getValueMapping().put(origKey,
									originalMap.get(origKey));
							arrangementModel.addValue(slider, origKey,
									originalMap.get(origKey));
						} else {
							for (final Entry<ParameterModel, Collection<Slider>> entry : arrangementModel
									.getMainArrangement().entrySet()) {
								for (final Slider otherSlider : entry
										.getValue()) {
									if (otherSlider.equals(slider)) {
										otherSlider.getValueMapping().remove(
												origKey);
									}
								}
							}
							slider.getValueMapping().remove(origKey);
							arrangementModel.removeValue(slider, origKey);
						}
						volatileModel
								.actionPerformed(new ActionEvent(
										this,
										(int) (System.currentTimeMillis() & 0xffffffff),
										"value handling."));
						viewModel
								.actionPerformed(new ActionEvent(
										this,
										(int) (System.currentTimeMillis() & 0xffffffff),
										"value handling."));
						arrangementModel
								.actionPerformed(new ActionEvent(
										this,
										(int) (System.currentTimeMillis() & 0xffffffff),
										"value handling."));
					}
				});
				ret.add(button);
			}
			return ret;
		}
		throw new UnsupportedOperationException("Sorry, not supported yet.");
	}

	private Component createSliderComboBox(final Slider slider) {
		final List<ParameterModel> parameters = slider.getParameters();
		if (parameters.size() == 1) {
			final JComboBox ret = new JComboBox();
			for (final ParameterModel model : parameters) {
				for (final Entry<Integer, Map<ParameterModel, Object>> entry : slider
						.getValueMapping().entrySet()) {
					ret.addItem(entry.getValue().get(model));
				}
			}
			ret.setSelectedIndex(view.getVolatileModel().getSliderPositions()
					.get(slider).intValue() - 1);
			// ret.setEditable(false);
			ret.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					view.getVolatileModel().setSliderPosition(slider,
							Integer.valueOf(ret.getSelectedIndex() + 1));
				}
			});
			return ret;
		}
		throw new UnsupportedOperationException("Sorry, not supported yet.");
		// return null;
	}

	private void addBorderButton(final HeatmapNodeView origView,
			final GridBagLayout gbLayout, final JCheckBox checkbox,
			final int row, final int col, final int boolPos) {
		final GridBagConstraints showBorderConstraint = new GridBagConstraints();
		setSelection(origView.getCurrentViewModel().getMain(), checkbox,
				boolPos);
		checkbox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				final ViewModel viewModel = origView.getCurrentViewModel();
				final ShapeModel origShapeModel = viewModel.getMain();
				final ArrangementModel origArrangementModel = origShapeModel
						.getArrangementModel();
				final ShapeModel shapeModel = new ShapeModel(
						origArrangementModel, origShapeModel
								.getPrimerParameters(), origShapeModel
								.getSecunderParameters(), origShapeModel
								.getAdditionalParameters(),
						boolPos == 0 ? checkbox.isSelected() : origShapeModel
								.isDrawBorder(), boolPos == 1 ? checkbox
								.isSelected() : origShapeModel
								.isDrawPrimaryBorders(),
						boolPos == 2 ? checkbox.isSelected() : origShapeModel
								.isDrawSecundaryBorders(),
						boolPos == 3 ? checkbox.isSelected() : origShapeModel
								.isDrawAdditionalBorders());
				origView.setCurrentViewModel(new ViewModel(viewModel,
						shapeModel));
			}
		});
		showBorderConstraint.gridx = col;
		showBorderConstraint.gridy = row;
		gbLayout.addLayoutComponent(checkbox, showBorderConstraint);
		add(checkbox, showBorderConstraint);
	}

	private void setSelection(final ShapeModel model, final JCheckBox checkbox,
			final int boolPos) {
		final boolean[] bools = new boolean[] { model.isDrawBorder(),
				model.isDrawPrimaryBorders(), model.isDrawSecundaryBorders(),
				model.isDrawAdditionalBorders() };
		checkbox.setSelected(bools[boolPos]);
	}

	public void setViewModel(final ViewModel model) {
		legendPanel.setViewModel(model);
	}

	public void setModel(final HeatmapNodeModel nodeModel) {
		try {
			combineParameters(nodeModel.possibleParameters);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		internalUpdateParameters();
	}

	private void internalUpdateParameters() {
		// TODO Auto-generated method stub
		paramaterSelection.setPossibleValues(view.getCurrentViewModel()
				.getMain().getArrangementModel().getTypeValuesMap());
	}

	private void combineParameters(
			final Collection<ParameterModel> possibleParameters) {
		view.getCurrentViewModel().getMain().getArrangementModel().mutate(
				possibleParameters);
		view.getCurrentViewModel().getMain().updateParameters(
				possibleParameters);
		view.getVolatileModel().mutateValues(
				view.getCurrentViewModel().getMain().getArrangementModel(),
				view.getNodeModel());
	}
}
